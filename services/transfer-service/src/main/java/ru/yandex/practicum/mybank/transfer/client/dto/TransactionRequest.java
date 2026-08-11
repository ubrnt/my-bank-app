package ru.yandex.practicum.mybank.transfer.client.dto;

import java.util.UUID;

public record TransactionRequest(
		UUID transactionUuid,
		String fromLogin,
		String toLogin,
		long amount
) {
}
