package ru.yandex.practicum.mybank.front.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybank.chassis.client.CircuitBreakerPolicy;
import ru.yandex.practicum.mybank.chassis.client.ServiceClientFactory;

@Configuration
public class GatewayClientConfig {

	private static final String GATEWAY_ID = "gateway";
	private static final String REGISTRATION_ID = "keycloak";

	@Bean
	public OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository clientRegistrations,
			OAuth2AuthorizedClientRepository authorizedClients) {
		return new DefaultOAuth2AuthorizedClientManager(clientRegistrations, authorizedClients);
	}

	@Bean
	public RestClient gatewayRestClient(ServiceClientFactory factory) {
		return factory.restClient(GATEWAY_ID, REGISTRATION_ID, CircuitBreakerPolicy.TRANSPORT_ONLY);
	}
}
