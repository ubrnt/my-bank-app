package ru.yandex.practicum.mybank.notifications.outbox;

import java.util.UUID;

public class NotificationDeliveryException extends RuntimeException {

	public NotificationDeliveryException(UUID eventUuid, Throwable cause) {
		super("Failed to deliver notification for event " + eventUuid, cause);
	}
}
