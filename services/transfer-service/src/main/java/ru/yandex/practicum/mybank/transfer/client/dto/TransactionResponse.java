package ru.yandex.practicum.mybank.transfer.client.dto;

import java.util.UUID;

public record TransactionResponse(
		UUID uuid,
		String type,
		TransactionOperation operation
) {
}
