package ru.yandex.practicum.mybank.cash.client.dto;

import java.util.List;
import java.util.UUID;

public record TransactionResponse(
		UUID uuid,
		String type,
		List<TransactionOperation> operations
) {

	public TransactionOperation operation() {
		if (operations.size() != 1) {
			throw new IllegalStateException(
					"Cash transaction must have exactly one operation, got " + operations.size());
		}

		return operations.getFirst();
	}
}
