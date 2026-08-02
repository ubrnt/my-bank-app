package ru.yandex.practicum.mybank.accounts.dto;

import ru.yandex.practicum.mybank.accounts.domain.Account;
import ru.yandex.practicum.mybank.accounts.domain.Customer;

import java.time.LocalDate;

public record AccountResponse(
		String login,
		String name,
		LocalDate birthdate,
		String number,
		long balance
) {

	public static AccountResponse of(Account account) {
		Customer customer = account.getCustomer();
		return new AccountResponse(
				customer.getLogin(),
				customer.getName(),
				customer.getBirthdate(),
				account.getNumber(),
				account.getBalance());
	}
}
