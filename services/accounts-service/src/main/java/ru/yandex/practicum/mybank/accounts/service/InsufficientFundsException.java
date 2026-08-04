package ru.yandex.practicum.mybank.accounts.service;

public class InsufficientFundsException extends RuntimeException {

	public InsufficientFundsException(String login, long requested, long available) {
		super("Insufficient funds on account of " + login + ": requested " + requested + ", available " + available);
	}
}
