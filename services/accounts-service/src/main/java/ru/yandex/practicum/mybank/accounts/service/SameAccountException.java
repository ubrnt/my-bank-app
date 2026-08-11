package ru.yandex.practicum.mybank.accounts.service;

public class SameAccountException extends RuntimeException {

	public SameAccountException(String login) {
		super("Transfer from and to the same account of " + login);
	}
}
