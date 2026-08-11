package ru.yandex.practicum.mybank.cash.dto;

import ru.yandex.practicum.mybank.cash.service.dto.CashOperationDto;

import java.util.UUID;

public record CashOperationResponse(
		UUID uuid,
		String type,
		long amount,
		String status
) {

	public static CashOperationResponse of(CashOperationDto operation) {
		return new CashOperationResponse(
				operation.uuid(),
				operation.type().name().toLowerCase(),
				operation.amount(),
				operation.status().name().toLowerCase());
	}
}
