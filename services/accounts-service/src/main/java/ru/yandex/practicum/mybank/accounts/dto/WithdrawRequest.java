package ru.yandex.practicum.mybank.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record WithdrawRequest(
		@NotNull UUID transactionId,
		@NotBlank String login,
		@Positive long amount
) {
}
