package ru.yandex.practicum.mybank.cash;

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
import ru.yandex.practicum.mybank.cash.client.AccountsClient;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionOperation;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionRequest;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionResponse;
import ru.yandex.practicum.mybank.notifications.outbox.NotificationsOutboxRelay;

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

	private static final UUID ACCOUNT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID CUSTOMER_UUID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");

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

		when(accountsClient.deposit(any())).thenAnswer(invocation -> {
			TransactionRequest request = invocation.getArgument(0, TransactionRequest.class);

			return new TransactionResponse(request.transactionUuid(), "deposit",
					List.of(new TransactionOperation("deposit", "40817810000000000001",
							ACCOUNT_UUID, CUSTOMER_UUID, null, null, null, request.amount(), 25500L)));
		});
	}

	@Test
	void relayedDepositEventMatchesNotificationsContract() throws Exception {
		mockMvc.perform(post("/api/cash/deposit")
						.with(jwt().jwt(jwt -> jwt
								.claim("preferred_username", "user1")
								.claim("scope", "cash:write")))
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", UUID.randomUUID())
						.content("""
								{"amount": 500}
								"""))
				.andExpect(status().isOk());

		notificationsOutboxRelay.relayPending();

		String status = jdbcTemplate.queryForObject("select status from notifications_outbox", String.class);
		assertThat(status).isEqualTo("PROCESSED");
	}
}
