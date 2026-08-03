package ru.yandex.practicum.mybank.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.yandex.practicum.mybank.accounts.validation.Adult;

import java.time.LocalDate;

public record UpdateProfileRequest(
		@NotBlank(message = "Не должно быть пустым")
		String name,

		@NotNull(message = "Не должно быть пустым")
		@Adult
		LocalDate birthdate
) {
}
