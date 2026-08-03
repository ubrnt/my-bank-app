package ru.yandex.practicum.mybank.accounts.service;

public class CustomerAccountNotFoundException extends RuntimeException {

	public CustomerAccountNotFoundException(String login) {
		super("Customer account not found for login " + login);
	}
}
