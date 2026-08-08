package ru.yandex.practicum.mybank.transfer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mybank.notifications.outbox.NotificationsOutboxRelay;
import ru.yandex.practicum.mybank.transfer.client.AccountsClient;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionOperation;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionRequest;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionResponse;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureStubRunner(
		ids = "ru.yandex.practicum:notifications-service:+:stubs",
		stubsMode = StubRunnerProperties.StubsMode.LOCAL)
@Import({PostgresContainerConfig.class, FakeTokenConfig.class})
class NotificationsContractIntegrationTest {

	private static final String FROM_NUMBER = "40817810000000000001";
	private static final UUID FROM_ACCOUNT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID FROM_CUSTOMER_UUID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
	private static final String TO_NUMBER = "40817810000000000002";
	private static final UUID TO_ACCOUNT_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID TO_CUSTOMER_UUID = UUID.fromString("aaaaaaaa-2222-2222-2222-222222222222");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NotificationsOutboxRelay notificationsOutboxRelay;

	@MockitoBean
	private AccountsClient accountsClient;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@BeforeEach
	void resetData() {
		jdbcTemplate.execute("truncate table notifications_outbox restart identity");

		when(accountsClient.transfer(any())).thenAnswer(invocation -> {
			TransactionRequest request = invocation.getArgument(0, TransactionRequest.class);

			TransactionOperation sent = new TransactionOperation("withdraw",
					TO_NUMBER, TO_ACCOUNT_UUID, TO_CUSTOMER_UUID,
					FROM_NUMBER, FROM_ACCOUNT_UUID, FROM_CUSTOMER_UUID,
					request.amount(), 24500L);
			TransactionOperation received = new TransactionOperation("deposit",
					TO_NUMBER, TO_ACCOUNT_UUID, TO_CUSTOMER_UUID,
					FROM_NUMBER, FROM_ACCOUNT_UUID, FROM_CUSTOMER_UUID,
					request.amount(), 5500L);

			return new TransactionResponse(request.transactionUuid(), "transfer", List.of(sent, received));
		});
	}

	@Test
	void relayedTransferEventsMatchNotificationsContract() throws Exception {
		mockMvc.perform(post("/api/transfers")
						.with(jwt().jwt(jwt -> jwt
								.claim("preferred_username", "user1")
								.claim("scope", "transfer:write")))
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", UUID.randomUUID())
						.content("""
								{"toLogin": "user2", "amount": 500}
								"""))
				.andExpect(status().isOk());

		notificationsOutboxRelay.relayPending();

		List<String> statuses = jdbcTemplate.queryForList(
				"select status from notifications_outbox order by id", String.class);
		assertThat(statuses).containsExactly("PROCESSED", "PROCESSED");
	}
}
