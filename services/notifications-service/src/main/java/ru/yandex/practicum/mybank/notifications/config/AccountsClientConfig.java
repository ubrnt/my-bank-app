package ru.yandex.practicum.mybank.notifications.config;

import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class AccountsClientConfig {

	private static final String CLIENT_REGISTRATION_ID = "accounts";
	private static final String BASE_URL = "http://accounts-service";

	@Bean
	@LoadBalanced
	public RestClient.Builder loadBalancedRestClientBuilder(RestClientBuilderConfigurer configurer) {
		return configurer.configure(RestClient.builder());
	}

	@Bean
	public OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository clientRegistrations,
			OAuth2AuthorizedClientService authorizedClients) {
		return new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrations, authorizedClients);
	}

	@Bean
	public RestClient accountsRestClient(RestClient.Builder builder, OAuth2AuthorizedClientManager clientManager) {
		OAuth2ClientHttpRequestInterceptor interceptor = new OAuth2ClientHttpRequestInterceptor(clientManager);
		interceptor.setClientRegistrationIdResolver(request -> CLIENT_REGISTRATION_ID);

		return builder.baseUrl(BASE_URL)
				.requestInterceptor(interceptor)
				.build();
	}
}
