package ru.yandex.practicum.mybank.cash.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.mybank.cash.client.TransactionRejectedException;
import ru.yandex.practicum.mybank.cash.service.AccountsServiceUnavailableException;
import ru.yandex.practicum.mybank.cash.service.DuplicateRequestException;
import ru.yandex.practicum.mybank.cash.service.IdempotencyKeyConflictException;
import ru.yandex.practicum.mybank.chassis.web.BaseExceptionHandler;
import ru.yandex.practicum.mybank.chassis.web.ErrorResponse;

@RestControllerAdvice
public class ApiExceptionHandler extends BaseExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(TransactionRejectedException.class)
	public ResponseEntity<ErrorResponse> handleTransactionRejected(TransactionRejectedException exception) {
		log.warn("Accounts rejected the operation: {}", exception.getMessage());

		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
				.body(new ErrorResponse(exception.getCode(), "Operation rejected by accounts-service"));
	}

	@ExceptionHandler(IdempotencyKeyConflictException.class)
	public ResponseEntity<ErrorResponse> handleIdempotencyKeyConflict(IdempotencyKeyConflictException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ErrorResponse("idempotency_key_conflict", exception.getMessage()));
	}

	@ExceptionHandler(DuplicateRequestException.class)
	public ResponseEntity<ErrorResponse> handleDuplicateRequest(DuplicateRequestException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ErrorResponse("duplicate_request", exception.getMessage()));
	}

	@ExceptionHandler(AccountsServiceUnavailableException.class)
	public ResponseEntity<ErrorResponse> handleAccountsServiceUnavailable(AccountsServiceUnavailableException exception) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ErrorResponse("accounts_unavailable", exception.getMessage()));
	}
}
