package ru.yandex.practicum.mybank.cash.service;

public class AccountsServiceUnavailableException extends RuntimeException {

	public AccountsServiceUnavailableException(Throwable cause) {
		super("Accounts service is unavailable", cause);
	}
}
