package ru.yandex.practicum.mybank.notifications.client;

import java.util.UUID;

public class UnknownRecipientException extends RuntimeException {

	public static final String CODE = "unknown_recipient";

	public UnknownRecipientException(UUID customerUuid, Throwable cause) {
		super("Failed to find customer " + customerUuid + " in accounts-service", cause);
	}
}
