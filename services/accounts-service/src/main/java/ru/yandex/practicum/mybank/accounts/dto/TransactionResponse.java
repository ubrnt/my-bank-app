package ru.yandex.practicum.mybank.accounts.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.yandex.practicum.mybank.accounts.service.dto.OperationDto;
import ru.yandex.practicum.mybank.accounts.service.dto.TransactionDto;

import java.util.List;
import java.util.UUID;

public record TransactionResponse(
		UUID id,
		String type,
		List<Operation> operations
) {

	//todo ubrnt, think whether we need to have dto's at all
	public static TransactionResponse of(TransactionDto transaction, String ownerLogin) {
		return new TransactionResponse(
				transaction.id(),
				transaction.type().name().toLowerCase(),
				transaction.operations().stream()
						.map(operation -> owns(operation, ownerLogin)
								? Operation.of(operation)
								: Operation.masked(operation))
						.toList());
	}

	private static boolean owns(OperationDto operation, String ownerLogin) {
		return operation.login().equals(ownerLogin);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Operation(
			String direction,
			String login,
			String number,
			long amount,
			Long balanceAfter
	) {

		private static Operation of(OperationDto operation) {
			return new Operation(
					direction(operation),
					operation.login(),
					operation.number(),
					operation.amount(),
					operation.balanceAfter());
		}

		private static Operation masked(OperationDto operation) {
			return new Operation(
					direction(operation),
					operation.login(),
					null,
					operation.amount(),
					null);
		}

		private static String direction(OperationDto operation) {
			return operation.direction().name().toLowerCase();
		}
	}
}
