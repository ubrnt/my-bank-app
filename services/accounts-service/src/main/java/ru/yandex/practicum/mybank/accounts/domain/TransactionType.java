package ru.yandex.practicum.mybank.accounts.domain;

public enum TransactionType {
	DEPOSIT(1),
	WITHDRAW(1),
	TRANSFER(2);

	private final int operationCount;

	TransactionType(int operationCount) {
		this.operationCount = operationCount;
	}

	public int operationCount() {
		return operationCount;
	}
}
