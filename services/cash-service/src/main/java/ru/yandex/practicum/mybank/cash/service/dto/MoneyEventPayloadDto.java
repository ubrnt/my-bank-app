package ru.yandex.practicum.mybank.cash.service.dto;

import ru.yandex.practicum.mybank.cash.client.dto.TransactionOperation;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionResponse;

import java.util.UUID;

public record MoneyEventPayloadDto(
		UUID transactionUuid,
		String type,
		Operation operation
) {

	public static MoneyEventPayloadDto of(TransactionResponse transaction) {
		return new MoneyEventPayloadDto(transaction.uuid(), transaction.type(),
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
