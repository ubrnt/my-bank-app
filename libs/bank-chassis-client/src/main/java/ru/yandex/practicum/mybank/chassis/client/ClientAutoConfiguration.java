package ru.yandex.practicum.mybank.chassis.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

@AutoConfiguration(
		afterName = "org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration",
		beforeName = "org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration")
@ConditionalOnBean(ClientRegistrationRepository.class)
@EnableConfigurationProperties(ClientsProperties.class)
public class ClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public RestClient.Builder restClientBuilder(RestClientBuilderConfigurer configurer) {
		return configurer.configure(RestClient.builder());
	}

	@Bean
	@ConditionalOnMissingBean
	public CircuitBreakerStateLogger circuitBreakerStateLogger(CircuitBreakerRegistry circuitBreakerRegistry) {
		return new CircuitBreakerStateLogger(circuitBreakerRegistry);
	}

	@Bean
	@ConditionalOnMissingBean
	public OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository clientRegistrations,
			OAuth2AuthorizedClientService authorizedClients) {
		return new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrations, authorizedClients);
	}

	@Bean
	@ConditionalOnMissingBean
	public ServiceClientFactory serviceClientFactory(RestClient.Builder builder,
			OAuth2AuthorizedClientManager clientManager, CircuitBreakerFactory<?, ?> circuitBreakerFactory,
			ClientsProperties clients) {
		return new ServiceClientFactory(builder, clientManager, circuitBreakerFactory, clients);
	}
}
