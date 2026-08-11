package ru.yandex.practicum.mybank.cash.service.dto;

import ru.yandex.practicum.mybank.cash.domain.CashOperation;
import ru.yandex.practicum.mybank.cash.domain.CashOperationStatus;
import ru.yandex.practicum.mybank.cash.domain.CashOperationType;

import java.util.UUID;

public record CashOperationDto(
		UUID uuid,
		CashOperationType type,
		long amount,
		CashOperationStatus status
) {

	public static CashOperationDto of(CashOperation operation) {
		return new CashOperationDto(operation.getUuid(), operation.getType(),
				operation.getAmount(), operation.getStatus());
	}
}
