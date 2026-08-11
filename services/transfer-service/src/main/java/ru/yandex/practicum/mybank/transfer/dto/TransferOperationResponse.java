package ru.yandex.practicum.mybank.transfer.dto;

import ru.yandex.practicum.mybank.transfer.service.dto.TransferOperationDto;

import java.util.UUID;

public record TransferOperationResponse(
		UUID uuid,
		long amount,
		String status
) {

	public static TransferOperationResponse of(TransferOperationDto operation) {
		return new TransferOperationResponse(
				operation.uuid(),
				operation.amount(),
				operation.status().name().toLowerCase());
	}
}
