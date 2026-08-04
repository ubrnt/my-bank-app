package ru.yandex.practicum.mybank.accounts.service.dto;

import ru.yandex.practicum.mybank.accounts.domain.OperationDirection;

public record OperationDto(
		OperationDirection direction,
		String login,
		String number,
		long amount,
		long balanceAfter
) {
}
