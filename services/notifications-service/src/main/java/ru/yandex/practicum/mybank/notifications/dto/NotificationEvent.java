package ru.yandex.practicum.mybank.notifications.dto;

import ru.yandex.practicum.mybank.notifications.domain.EventType;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

public record NotificationEvent(
		UUID eventUuid,
		EventType type,
		UUID recipientUuid,
		JsonNode payload
) {
}
