package ru.yandex.practicum.mybank.accounts.dto;

import ru.yandex.practicum.mybank.accounts.service.dto.OperationDto;
import ru.yandex.practicum.mybank.accounts.service.dto.TransactionDto;

import java.util.UUID;

public record TransactionResponse(
		UUID uuid,
		String type,
		Operation operation
) {

	//todo ubrnt, think whether we need to have dto's at all
	public static TransactionResponse of(TransactionDto transaction) {
		return new TransactionResponse(
				transaction.uuid(),
				transaction.type().name().toLowerCase(),
				Operation.of(transaction.operation()));
	}

	public record Operation(
			String direction,
			String fromNumber,
			UUID fromAccountUuid,
			UUID fromCustomerUuid,
			String toNumber,
			UUID toAccountUuid,
			UUID toCustomerUuid,
			long amount,
			long balanceAfter
	) {

		private static Operation of(OperationDto operation) {
			return new Operation(
					operation.direction().name().toLowerCase(),
					operation.fromNumber(),
					operation.fromAccountUuid(),
					operation.fromCustomerUuid(),
					operation.toNumber(),
					operation.toAccountUuid(),
					operation.toCustomerUuid(),
					operation.amount(),
					operation.balanceAfter());
		}
	}
}
