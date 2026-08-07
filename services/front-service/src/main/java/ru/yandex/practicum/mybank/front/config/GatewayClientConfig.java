package ru.yandex.practicum.mybank.front.config;

import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class GatewayClientConfig {

	private static final String REGISTRATION_ID = "keycloak";

	@Bean
	@LoadBalanced
	public RestClient.Builder loadBalancedRestClientBuilder(RestClientBuilderConfigurer configurer) {
		return configurer.configure(RestClient.builder());
	}

	@Bean
	public OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository clientRegistrations,
			OAuth2AuthorizedClientRepository authorizedClients) {
		return new DefaultOAuth2AuthorizedClientManager(clientRegistrations, authorizedClients);
	}

	@Bean
	public RestClient gatewayRestClient(RestClient.Builder builder, OAuth2AuthorizedClientManager clientManager) {
		OAuth2ClientHttpRequestInterceptor tokenInterceptor = new OAuth2ClientHttpRequestInterceptor(clientManager);
		tokenInterceptor.setClientRegistrationIdResolver(request -> REGISTRATION_ID);

		return builder.clone()
				.baseUrl("http://gateway-service")
				.requestInterceptor(tokenInterceptor)
				.build();
	}
}
