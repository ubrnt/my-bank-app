package ru.yandex.practicum.mybank.accounts.service;

public class SameAccountException extends RuntimeException {

	public SameAccountException(String login) {
		super("Transfer source and target are the same account of " + login);
	}
}
