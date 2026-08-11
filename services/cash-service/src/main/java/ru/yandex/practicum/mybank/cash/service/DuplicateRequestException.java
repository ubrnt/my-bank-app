package ru.yandex.practicum.mybank.cash.service;

import java.util.UUID;

public class DuplicateRequestException extends RuntimeException {

	public DuplicateRequestException(UUID idempotencyKey) {
		super("Operation %s is already in progress".formatted(idempotencyKey));
	}
}
