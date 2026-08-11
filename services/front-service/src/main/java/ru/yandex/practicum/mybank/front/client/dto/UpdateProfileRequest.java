package ru.yandex.practicum.mybank.front.client.dto;

import java.time.LocalDate;

public record UpdateProfileRequest(
		String name,
		LocalDate birthdate
) {
}
