package ru.yandex.practicum.mybank.front.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientPropertiesMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class ClientRegistrationConfig {

	private static final String END_SESSION_ENDPOINT = "end_session_endpoint";

	@Bean
	public ClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties properties,
			@Value("${mybank.oauth2.end-session-uri:}") String endSessionUri) {
		List<ClientRegistration> registrations = new OAuth2ClientPropertiesMapper(properties).asClientRegistrations()
				.values().stream()
				.map(registration -> withEndSessionEndpoint(registration, endSessionUri))
				.toList();

		return new InMemoryClientRegistrationRepository(registrations);
	}

	private ClientRegistration withEndSessionEndpoint(ClientRegistration registration, String endSessionUri) {
		Map<String, Object> metadata = registration.getProviderDetails().getConfigurationMetadata();

		if (endSessionUri.isEmpty() || metadata.containsKey(END_SESSION_ENDPOINT)) {
			return registration;
		}

		return ClientRegistration.withClientRegistration(registration)
				.providerConfigurationMetadata(Map.of(END_SESSION_ENDPOINT, endSessionUri))
				.build();
	}
}
