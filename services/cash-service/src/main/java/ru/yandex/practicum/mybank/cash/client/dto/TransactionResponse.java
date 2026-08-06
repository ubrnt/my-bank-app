package ru.yandex.practicum.mybank.cash.client.dto;

import java.util.UUID;

public record TransactionResponse(
		UUID uuid,
		TransactionOperation operation
) {
}
