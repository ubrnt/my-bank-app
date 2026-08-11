package ru.yandex.practicum.mybank.accounts.service.dto;

import java.time.LocalDate;

public record CustomerAccountDto(
		String login,
		String name,
		LocalDate birthdate,
		String number,
		long balance
) {
}
