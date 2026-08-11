package ru.yandex.practicum.mybank.accounts.service;

import java.util.UUID;

public class TransactionConflictException extends RuntimeException {

	public TransactionConflictException(UUID transactionUuid) {
		super("Transaction " + transactionUuid + " was already applied with different details");
	}
}
