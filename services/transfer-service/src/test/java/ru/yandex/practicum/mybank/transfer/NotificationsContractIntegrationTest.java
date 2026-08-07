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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureStubRunner(
		ids = {"ru.yandex.practicum:accounts-service:+:stubs", "ru.yandex.practicum:notifications-service:+:stubs"},
		stubsMode = StubRunnerProperties.StubsMode.LOCAL)
@Import({PostgresContainerConfig.class, FakeTokenConfig.class})
class NotificationsContractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NotificationsOutboxRelay notificationsOutboxRelay;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@BeforeEach
	void resetData() {
		jdbcTemplate.execute("truncate table notifications_outbox restart identity");
	}

	@Test
	void relayedTransferEventsMatchNotificationsContract() throws Exception {
		mockMvc.perform(post("/api/transfers")
						.with(jwt().jwt(jwt -> jwt
								.claim("preferred_username", "user1")
								.claim("scope", "transfer:write")))
						.contentType(MediaType.APPLICATION_JSON)
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
