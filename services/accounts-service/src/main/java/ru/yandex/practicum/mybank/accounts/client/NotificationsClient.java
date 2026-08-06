package ru.yandex.practicum.mybank.accounts.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.yandex.practicum.mybank.accounts.client.dto.NotificationRequest;

@Component
public class NotificationsClient {

	private static final String NOTIFICATIONS_PATH = "/api/notifications";

	private final RestClient restClient;

	public NotificationsClient(RestClient notificationsRestClient) {
		this.restClient = notificationsRestClient;
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
