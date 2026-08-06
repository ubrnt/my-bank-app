package ru.yandex.practicum.mybank.transfer.service.dto;

import ru.yandex.practicum.mybank.transfer.client.dto.TransactionOperation;

import java.util.UUID;

public record MoneyEventPayloadDto(
		UUID transactionUuid,
		String type,
		Operation operation
) {

	public static MoneyEventPayloadDto of(UUID transactionUuid, String type, TransactionOperation operation) {
		return new MoneyEventPayloadDto(transactionUuid, type, Operation.of(operation));
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

		private static Operation of(TransactionOperation operation) {
			return new Operation(
					operation.direction(),
					operation.fromNumber(),
					operation.fromAccountUuid(),
					operation.fromCustomerUuid(),
					operation.toNumber(),
					operation.toAccountUuid(),
					operation.toCustomerUuid(),
					require(operation.amount(), "amount"),
					require(operation.balanceAfter(), "balanceAfter"));
		}

		private static long require(Long value, String field) {
			if (value == null) {
				throw new IllegalStateException("Accounts operation lacks '%s'".formatted(field));
			}

			return value;
		}
	}
}
