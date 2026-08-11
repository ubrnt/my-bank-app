package ru.yandex.practicum.mybank.front.client.dto;

import java.time.LocalDate;

public record CustomerResponse(
		String login,
		String name,
		LocalDate birthdate,
		String number,
		long balance
) {
}
