package ru.yandex.practicum.mybank.accounts.client.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import ru.yandex.practicum.mybank.accounts.domain.EventType;

import java.util.UUID;

public record NotificationRequest(
		UUID eventUuid,
		EventType type,
		@JsonRawValue String recipient,
		@JsonRawValue String payload
) {
}
