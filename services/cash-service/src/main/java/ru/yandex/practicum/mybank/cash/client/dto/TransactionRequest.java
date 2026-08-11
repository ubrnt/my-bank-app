package ru.yandex.practicum.mybank.cash.client.dto;

import java.util.UUID;

public record TransactionRequest(
		UUID transactionUuid,
		String login,
		long amount
) {
}
