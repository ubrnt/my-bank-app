package ru.yandex.practicum.mybank.accounts.service;

public class BalanceLimitExceededException extends RuntimeException {

	public BalanceLimitExceededException(String login, long requested, long balance) {
		super("Balance limit exceeded on account of " + login + ": requested " + requested + ", balance " + balance);
	}
}
