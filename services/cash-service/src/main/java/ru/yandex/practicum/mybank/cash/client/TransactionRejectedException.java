package ru.yandex.practicum.mybank.cash.client;

public class TransactionRejectedException extends RuntimeException {

	private final String code;

	public TransactionRejectedException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
