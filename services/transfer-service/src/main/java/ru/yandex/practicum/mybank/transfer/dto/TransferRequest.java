package ru.yandex.practicum.mybank.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferRequest(
		@NotBlank
		String toLogin,

		@NotNull
		@Positive
		Long amount
) {
}
