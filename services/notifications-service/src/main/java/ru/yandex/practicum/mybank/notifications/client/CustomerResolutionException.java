package ru.yandex.practicum.mybank.notifications.client;

import java.util.UUID;

public class CustomerResolutionException extends RuntimeException {

	public CustomerResolutionException(UUID customerUuid, Throwable cause) {
		super("Failed to resolve customer " + customerUuid, cause);
	}
}
