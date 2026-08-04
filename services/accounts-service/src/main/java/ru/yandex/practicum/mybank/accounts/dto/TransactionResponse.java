package ru.yandex.practicum.mybank.accounts.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.yandex.practicum.mybank.accounts.service.dto.OperationDto;
import ru.yandex.practicum.mybank.accounts.service.dto.TransactionDto;

import java.util.UUID;

public record TransactionResponse(
		UUID id,
		String type,
		Operation operation
) {

	//todo ubrnt, think whether we need to have dto's at all
	public static TransactionResponse of(TransactionDto transaction) {
		return new TransactionResponse(
				transaction.id(),
				transaction.type().name().toLowerCase(),
				Operation.of(transaction.operation()));
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Operation(
			String direction,
			String fromLogin,
			String fromNumber,
			String toLogin,
			String toNumber,
			long amount,
			long balanceAfter
	) {

		private static Operation of(OperationDto operation) {
			return new Operation(
					operation.direction().name().toLowerCase(),
					operation.fromLogin(),
					operation.fromNumber(),
					operation.toLogin(),
					operation.toNumber(),
					operation.amount(),
					operation.balanceAfter());
		}
	}
}
