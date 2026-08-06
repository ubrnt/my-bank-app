package ru.yandex.practicum.mybank.transfer.client.dto;

import java.util.List;
import java.util.UUID;

public record TransactionResponse(
		UUID uuid,
		String type,
		List<TransactionOperation> operations
) {

	public TransactionOperation sent() {
		return operation("withdraw");
	}

	public TransactionOperation received() {
		return operation("deposit");
	}

	private TransactionOperation operation(String direction) {
		return operations.stream()
				.filter(operation -> direction.equals(operation.direction()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException(
						"Transfer response lacks the '%s' operation".formatted(direction)));
	}
}
