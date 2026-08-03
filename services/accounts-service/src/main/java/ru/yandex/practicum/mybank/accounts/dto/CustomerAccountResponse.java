package ru.yandex.practicum.mybank.accounts.dto;

import ru.yandex.practicum.mybank.accounts.domain.Account;
import ru.yandex.practicum.mybank.accounts.domain.Customer;
import ru.yandex.practicum.mybank.accounts.domain.CustomerAccount;

import java.time.LocalDate;

public record CustomerAccountResponse(
		String login,
		String name,
		LocalDate birthdate,
		String number,
		long balance
) {

	public static CustomerAccountResponse of(CustomerAccount customerAccount) {
		Customer customer = customerAccount.customer();
		Account account = customerAccount.account();
		return new CustomerAccountResponse(
				customer.getLogin(),
				customer.getName(),
				customer.getBirthdate(),
				account.getNumber(),
				account.getBalance());
	}
}
