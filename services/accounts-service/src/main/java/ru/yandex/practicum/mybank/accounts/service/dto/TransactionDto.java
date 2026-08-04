package ru.yandex.practicum.mybank.accounts.service.dto;

import ru.yandex.practicum.mybank.accounts.domain.TransactionType;

import java.util.UUID;

public record TransactionDto(
		UUID id,
		TransactionType type,
		OperationDto operation
) {
}
