package ru.yandex.practicum.mybank.cash;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;

@TestConfiguration(proxyBeanMethods = false)
class FakeTokenConfig {

	@Bean
	@Primary
	public OAuth2AuthorizedClientManager fakeAuthorizedClientManager(ClientRegistrationRepository registrations) {
		ClientRegistration registration = registrations.findByRegistrationId("accounts-service");
		OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
				"fake-token", Instant.now(), Instant.now().plusSeconds(3600));

		return request -> new OAuth2AuthorizedClient(registration, "cash-service", token);
	}
}
