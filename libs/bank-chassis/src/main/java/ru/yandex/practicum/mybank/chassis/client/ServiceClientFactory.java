package ru.yandex.practicum.mybank.chassis.client;

import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

public class ServiceClientFactory {

	private final RestClient.Builder builder;
	private final OAuth2AuthorizedClientManager clientManager;
	private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
	private final ClientHttpRequestInterceptor loadBalancerInterceptor;

	public ServiceClientFactory(RestClient.Builder builder, OAuth2AuthorizedClientManager clientManager,
			CircuitBreakerFactory<?, ?> circuitBreakerFactory,
			ClientHttpRequestInterceptor loadBalancerInterceptor) {
		this.builder = builder;
		this.clientManager = clientManager;
		this.circuitBreakerFactory = circuitBreakerFactory;
		this.loadBalancerInterceptor = loadBalancerInterceptor;
	}

	public RestClient restClient(String serviceId) {
		return restClient(serviceId, serviceId, CircuitBreakerPolicy.TRANSPORT_AND_SERVER_ERRORS);
	}

	public RestClient restClient(String serviceId, String registrationId) {
		return restClient(serviceId, registrationId, CircuitBreakerPolicy.TRANSPORT_AND_SERVER_ERRORS);
	}

	public RestClient restClient(String serviceId, String registrationId, CircuitBreakerPolicy policy) {
		OAuth2ClientHttpRequestInterceptor tokenInterceptor = new OAuth2ClientHttpRequestInterceptor(clientManager);
		tokenInterceptor.setClientRegistrationIdResolver(request -> registrationId);

		return builder.clone()
				.baseUrl("http://" + serviceId)
				.requestInterceptor(new ClientLoggingInterceptor(serviceId))
				.requestInterceptor(tokenInterceptor)
				.requestInterceptor(new CircuitBreakerInterceptor(circuitBreakerFactory, serviceId, policy))
				.requestInterceptor(loadBalancerInterceptor)
				.build();
	}
}
