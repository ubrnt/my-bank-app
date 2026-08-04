package ru.yandex.practicum.mybank.accounts.service.dto;

import ru.yandex.practicum.mybank.accounts.domain.OperationDirection;

public record OperationDto(
		OperationDirection direction,
		String fromLogin,
		String fromNumber,
		String toLogin,
		String toNumber,
		long amount,
		long balanceAfter
) {

	public static OperationDto deposit(String toLogin, String toNumber, long amount, long balanceAfter) {
		return new OperationDto(OperationDirection.DEPOSIT, null, null, toLogin, toNumber, amount, balanceAfter);
	}

	public static OperationDto withdrawal(String fromLogin, String fromNumber, long amount, long balanceAfter) {
		return new OperationDto(OperationDirection.WITHDRAW, fromLogin, fromNumber, null, null, amount, balanceAfter);
	}

	public static OperationDto sent(String fromLogin, String fromNumber, String toLogin, String toNumber,
			long amount, long balanceAfter) {
		return new OperationDto(OperationDirection.WITHDRAW, fromLogin, fromNumber, toLogin, toNumber,
				amount, balanceAfter);
	}

	public static OperationDto received(String fromLogin, String fromNumber, String toLogin, String toNumber,
			long amount, long balanceAfter) {
		return new OperationDto(OperationDirection.DEPOSIT, fromLogin, fromNumber, toLogin, toNumber,
				amount, balanceAfter);
	}
}
