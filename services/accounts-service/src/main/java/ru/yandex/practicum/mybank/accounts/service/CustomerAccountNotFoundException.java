package ru.yandex.practicum.mybank.accounts.service;

import java.util.UUID;

public class CustomerAccountNotFoundException extends RuntimeException {

	public CustomerAccountNotFoundException(String login) {
		super("Customer account not found for login " + login);
	}

	public CustomerAccountNotFoundException(UUID uuid) {
		super("Customer not found for uuid " + uuid);
	}
}
