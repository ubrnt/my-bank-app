package ru.yandex.practicum.mybank.accounts.service;

public class AccountNotFoundException extends RuntimeException {

	public AccountNotFoundException(String login) {
		super("Account not found for login " + login);
	}
}
