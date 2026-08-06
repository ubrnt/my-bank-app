package ru.yandex.practicum.mybank.transfer.service.dto;

import ru.yandex.practicum.mybank.transfer.domain.TransferOperation;
import ru.yandex.practicum.mybank.transfer.domain.TransferOperationStatus;

import java.util.UUID;

public record TransferOperationDto(
		UUID uuid,
		long amount,
		TransferOperationStatus status
) {

	public static TransferOperationDto of(TransferOperation operation) {
		return new TransferOperationDto(operation.getUuid(), operation.getAmount(), operation.getStatus());
	}
}
