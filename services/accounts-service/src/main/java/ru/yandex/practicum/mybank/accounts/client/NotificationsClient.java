package ru.yandex.practicum.mybank.accounts.client;

import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybank.accounts.client.dto.NotificationRequest;

@Component
public class NotificationsClient {

	private static final String CIRCUIT_BREAKER_ID = "notifications";
	private static final String NOTIFICATIONS_PATH = "/api/notifications";

	private final RestClient restClient;
	private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

	public NotificationsClient(RestClient notificationsRestClient, CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
		this.restClient = notificationsRestClient;
		this.circuitBreakerFactory = circuitBreakerFactory;
	}

	public void send(NotificationRequest request) {
		circuitBreakerFactory.create(CIRCUIT_BREAKER_ID).run(
				() -> restClient.post()
						.uri(NOTIFICATIONS_PATH)
						.body(request)
						.retrieve()
						.toBodilessEntity(),
				cause -> {
					throw new NotificationDeliveryException(request.eventUuid(), cause);
				});
	}
}
