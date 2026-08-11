package ru.yandex.practicum.mybank.accounts.service.dto;

import ru.yandex.practicum.mybank.accounts.domain.Account;
import ru.yandex.practicum.mybank.accounts.domain.OperationDirection;

import java.util.UUID;

public record OperationDto(
		OperationDirection direction,
		String fromNumber,
		UUID fromAccountUuid,
		UUID fromCustomerUuid,
		String toNumber,
		UUID toAccountUuid,
		UUID toCustomerUuid,
		long amount,
		long balanceAfter
) {

	public static OperationDto deposit(Account to, long amount, long balanceAfter) {
		return new OperationDto(OperationDirection.DEPOSIT, null, null, null,
				to.getNumber(), to.getUuid(), to.getCustomer().getUuid(), amount, balanceAfter);
	}

	public static OperationDto withdrawal(Account from, long amount, long balanceAfter) {
		return new OperationDto(OperationDirection.WITHDRAW,
				from.getNumber(), from.getUuid(), from.getCustomer().getUuid(),
				null, null, null, amount, balanceAfter);
	}

	public static OperationDto sent(Account from, Account to, long amount, long balanceAfter) {
		return new OperationDto(OperationDirection.WITHDRAW,
				from.getNumber(), from.getUuid(), from.getCustomer().getUuid(),
				to.getNumber(), to.getUuid(), to.getCustomer().getUuid(), amount, balanceAfter);
	}

	public static OperationDto received(Account from, Account to, long amount, long balanceAfter) {
		return new OperationDto(OperationDirection.DEPOSIT,
				from.getNumber(), from.getUuid(), from.getCustomer().getUuid(),
				to.getNumber(), to.getUuid(), to.getCustomer().getUuid(), amount, balanceAfter);
	}
}
