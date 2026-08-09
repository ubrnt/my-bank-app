package ru.yandex.practicum.mybank.transfer.service;

import java.util.UUID;

public class IdempotencyKeyConflictException extends RuntimeException {

	public IdempotencyKeyConflictException(UUID idempotencyKey) {
		super("Operation %s was already requested with other details".formatted(idempotencyKey));
	}
}
