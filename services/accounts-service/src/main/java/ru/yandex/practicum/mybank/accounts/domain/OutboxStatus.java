package ru.yandex.practicum.mybank.accounts.domain;

public enum OutboxStatus {
	PENDING,
	PROCESSING,
	PROCESSED,
	FAILED
}
