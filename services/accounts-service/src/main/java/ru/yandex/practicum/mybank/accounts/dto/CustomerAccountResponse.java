package ru.yandex.practicum.mybank.accounts.dto;

import ru.yandex.practicum.mybank.accounts.service.dto.CustomerAccountDto;

import java.time.LocalDate;

public record CustomerAccountResponse(
		String login,
		String name,
		LocalDate birthdate,
		String number,
		long balance
) {

	public static CustomerAccountResponse of(CustomerAccountDto customerAccount) {
		return new CustomerAccountResponse(
				customerAccount.login(),
				customerAccount.name(),
				customerAccount.birthdate(),
				customerAccount.number(),
				customerAccount.balance());
	}
}
