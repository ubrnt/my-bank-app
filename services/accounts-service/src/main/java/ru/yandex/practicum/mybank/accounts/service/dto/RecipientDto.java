package ru.yandex.practicum.mybank.accounts.service.dto;

import java.util.UUID;

public record RecipientDto(
		UUID uuid,
		String login
) {
}
