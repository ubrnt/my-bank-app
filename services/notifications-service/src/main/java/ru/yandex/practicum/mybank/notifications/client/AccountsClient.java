package ru.yandex.practicum.mybank.notifications.client;

import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybank.notifications.client.dto.CustomerResponse;

import java.util.UUID;

@Component
public class AccountsClient {

	private static final String CIRCUIT_BREAKER_ID = "accounts";

	private final RestClient restClient;
	private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

	public AccountsClient(RestClient accountsRestClient, CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
		this.restClient = accountsRestClient;
		this.circuitBreakerFactory = circuitBreakerFactory;
	}

	public CustomerResponse getCustomer(UUID customerUuid) {
		return circuitBreakerFactory.create(CIRCUIT_BREAKER_ID).run(
				() -> restClient.get()
						.uri("/api/customers/{uuid}", customerUuid)
						.retrieve()
						.body(CustomerResponse.class),
				cause -> {
					throw new CustomerResolutionException(customerUuid, cause);
				});
	}
}
