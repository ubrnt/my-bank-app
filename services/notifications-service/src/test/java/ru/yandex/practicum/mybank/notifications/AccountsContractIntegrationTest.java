package ru.yandex.practicum.mybank.notifications;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.mybank.notifications.client.CustomerResolutionException;
import ru.yandex.practicum.mybank.notifications.domain.EventType;
import ru.yandex.practicum.mybank.notifications.service.NotificationsService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureStubRunner(
		ids = "ru.yandex.practicum:accounts-service:+:stubs",
		stubsMode = StubRunnerProperties.StubsMode.LOCAL)
@Import({PostgresContainerConfig.class, AccountsContractIntegrationTest.FakeTokenConfig.class})
class AccountsContractIntegrationTest {

	private static final UUID EVENT_UUID = UUID.fromString("1b7f4a90-0d51-4c2e-9f77-0a1e5c3b0001");
	private static final UUID RECIPIENT_UUID = UUID.fromString("3f2a77c4-1e08-4a6b-8f21-9c0d5b7e1111");
	private static final UUID UNKNOWN_RECIPIENT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000404");

	private final JsonNode payload = JsonMapper.builder().build().readTree("""
			{"uuid": "3f2a77c4-1e08-4a6b-8f21-9c0d5b7e1111"}
			""");

	@Autowired
	private NotificationsService notificationsService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@BeforeEach
	void resetData() {
		jdbcTemplate.execute("truncate table notifications");
	}

	@Test
	void resolvesRecipientAgainstAccountsContractAndSavesNotification() {
		notificationsService.receive(EVENT_UUID, EventType.PROFILE_UPDATED, RECIPIENT_UUID, payload);

		String message = jdbcTemplate.queryForObject("select message from notifications", String.class);
		assertThat(message).isEqualTo("Данные профиля обновлены. Если это были не вы, обратитесь в банк");
	}

	@Test
	void failsToResolveRecipientUnknownToAccounts() {
		assertThatThrownBy(() ->
				notificationsService.receive(EVENT_UUID, EventType.PROFILE_UPDATED, UNKNOWN_RECIPIENT_UUID, payload))
				.isInstanceOf(CustomerResolutionException.class);

		Long count = jdbcTemplate.queryForObject("select count(*) from notifications", Long.class);
		assertThat(count).isZero();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FakeTokenConfig {

		@Bean
		@Primary
		public OAuth2AuthorizedClientManager fakeAuthorizedClientManager(ClientRegistrationRepository registrations) {
			ClientRegistration registration = registrations.findByRegistrationId("accounts");
			OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
					"fake-token", Instant.now(), Instant.now().plusSeconds(3600));

			return request -> new OAuth2AuthorizedClient(registration, "notifications-service", token);
		}
	}
}
