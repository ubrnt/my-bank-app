package ru.yandex.practicum.mybank.accounts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mybank.notifications.outbox.NotificationsOutboxRelay;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureStubRunner(
		ids = "ru.yandex.practicum:notifications-service:+:stubs",
		stubsMode = StubRunnerProperties.StubsMode.LOCAL)
@Import({PostgresContainerConfig.class, NotificationsContractIntegrationTest.FakeTokenConfig.class})
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
	void relayedProfileEventMatchesNotificationsContract() throws Exception {
		mockMvc.perform(put("/api/customers/me")
						.with(jwt().jwt(jwt -> jwt
								.claim("preferred_username", "user1")
								.claim("scope", "customer:write")))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Иванов Иван", "birthdate": "1990-01-15"}
								"""))
				.andExpect(status().isOk());

		notificationsOutboxRelay.relayPending();

		String status = jdbcTemplate.queryForObject("select status from notifications_outbox", String.class);
		assertThat(status).isEqualTo("PROCESSED");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FakeTokenConfig {

		@Bean
		@Primary
		public OAuth2AuthorizedClientManager fakeAuthorizedClientManager(ClientRegistrationRepository registrations) {
			ClientRegistration registration = registrations.findByRegistrationId("notifications-service");
			OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
					"fake-token", Instant.now(), Instant.now().plusSeconds(3600));

			return request -> new OAuth2AuthorizedClient(registration, "accounts-service", token);
		}
	}
}
