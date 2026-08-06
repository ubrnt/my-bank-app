package ru.yandex.practicum.mybank.cash.client.dto;

import java.util.UUID;

public record TransactionOperation(
		UUID toAccountUuid,
		UUID toCustomerUuid,
		UUID fromAccountUuid,
		UUID fromCustomerUuid
) {
}
