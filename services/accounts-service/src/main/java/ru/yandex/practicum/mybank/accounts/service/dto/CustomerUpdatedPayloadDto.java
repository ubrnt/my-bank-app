package ru.yandex.practicum.mybank.accounts.service.dto;

import java.util.UUID;

public record CustomerUpdatedPayloadDto(
		UUID customerUuid
) {
}
