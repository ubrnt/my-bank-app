package ru.yandex.practicum.mybank.accounts.service.dto;

import java.util.UUID;

public record CustomerDto(
		UUID uuid,
		String login,
		String name
) {
}
