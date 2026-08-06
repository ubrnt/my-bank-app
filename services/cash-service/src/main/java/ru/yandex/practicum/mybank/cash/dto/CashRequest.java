package ru.yandex.practicum.mybank.cash.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CashRequest(
		@NotNull
		@Positive
		Long amount
) {
}
