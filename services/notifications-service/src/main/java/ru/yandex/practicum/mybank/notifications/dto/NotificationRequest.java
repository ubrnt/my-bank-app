package ru.yandex.practicum.mybank.notifications.dto;

import jakarta.validation.constraints.NotNull;
import ru.yandex.practicum.mybank.notifications.domain.EventType;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

public record NotificationRequest(
		@NotNull UUID eventUuid,
		@NotNull EventType type,
		@NotNull UUID recipientUuid,
		@NotNull JsonNode payload
) {
}
