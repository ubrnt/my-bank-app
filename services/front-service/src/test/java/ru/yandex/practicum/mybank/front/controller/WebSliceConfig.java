package ru.yandex.practicum.mybank.front.controller;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import ru.yandex.practicum.mybank.front.config.SecurityConfig;

@TestConfiguration(proxyBeanMethods = false)
@EnableConfigurationProperties(WebEndpointProperties.class)
@Import({SecurityConfig.class, MessageRenderer.class})
class WebSliceConfig {

	@Bean
	public ClientRegistrationRepository clientRegistrationRepository() {
		return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("keycloak")
				.clientId("front-service")
				.clientSecret("front-service-secret")
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
				.authorizationUri("http://localhost:8180/realms/my-bank/protocol/openid-connect/auth")
				.tokenUri("http://localhost:8180/realms/my-bank/protocol/openid-connect/token")
				.build());
	}
}
