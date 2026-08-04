package ru.yandex.practicum.mybank.accounts.client;

import java.util.UUID;

public class NotificationDeliveryException extends RuntimeException {

	public NotificationDeliveryException(UUID eventUuid, Throwable cause) {
		super("Failed to deliver notification for event " + eventUuid, cause);
	}
}
