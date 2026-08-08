package ru.yandex.practicum.mybank.transfer.service;

import java.util.UUID;

public class DuplicateRequestException extends RuntimeException {

	public DuplicateRequestException(UUID idempotencyKey) {
		super("Operation %s is already in progress".formatted(idempotencyKey));
	}
}
