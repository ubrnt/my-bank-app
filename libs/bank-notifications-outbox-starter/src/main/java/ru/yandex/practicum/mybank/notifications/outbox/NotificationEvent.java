package ru.yandex.practicum.mybank.notifications.outbox;

import com.fasterxml.jackson.annotation.JsonRawValue;

import java.util.UUID;

public record NotificationEvent(
		UUID eventUuid,
		String type,
		UUID recipientUuid,
		@JsonRawValue String payload
) {
}
