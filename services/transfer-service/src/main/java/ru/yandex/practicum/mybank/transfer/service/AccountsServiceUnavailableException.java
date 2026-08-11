package ru.yandex.practicum.mybank.transfer.service;

public class AccountsServiceUnavailableException extends RuntimeException {

	public static final String CODE = "accounts_service_unavailable";

	public AccountsServiceUnavailableException(Throwable cause) {
		super("accounts-service is unavailable", cause);
	}
}
