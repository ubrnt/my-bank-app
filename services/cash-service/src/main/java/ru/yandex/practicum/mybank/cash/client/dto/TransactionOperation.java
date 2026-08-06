package ru.yandex.practicum.mybank.cash.client.dto;

import java.util.UUID;

public record TransactionOperation(
		String direction,
		String toNumber,
		UUID toAccountUuid,
		UUID toCustomerUuid,
		String fromNumber,
		UUID fromAccountUuid,
		UUID fromCustomerUuid,
		Long amount,
		Long balanceAfter
) {
}
