package ru.yandex.practicum.mybank.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record TransferRequest(
		@NotNull UUID transactionUuid,
		@NotBlank String fromLogin,
		@NotBlank String toLogin,
		@Positive long amount
) {
}
