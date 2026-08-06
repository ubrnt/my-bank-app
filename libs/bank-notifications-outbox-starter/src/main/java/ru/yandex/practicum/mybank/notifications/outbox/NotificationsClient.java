package ru.yandex.practicum.mybank.notifications.outbox;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class NotificationsClient {

	private static final String NOTIFICATIONS_PATH = "/api/notifications";

	private final RestClient restClient;

	public NotificationsClient(RestClient restClient) {
		this.restClient = restClient;
	}

	public void send(NotificationRequest request) {
		try {
			restClient.post()
					.uri(NOTIFICATIONS_PATH)
					.body(request)
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException e) {
			throw new NotificationDeliveryException(request.eventUuid(), e);
		}
	}
}
