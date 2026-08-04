package ru.yandex.practicum.mybank.accounts.service.dto;

import java.util.UUID;

public record EventPayloadDto(
		String login,
		UUID customerUuid,
		Object data
) {
}
