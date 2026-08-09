package ru.yandex.practicum.mybank.transfer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.mybank.chassis.web.BaseExceptionHandler;
import ru.yandex.practicum.mybank.chassis.web.ErrorResponse;
import ru.yandex.practicum.mybank.transfer.client.TransactionRejectedException;
import ru.yandex.practicum.mybank.transfer.service.AccountsServiceUnavailableException;
import ru.yandex.practicum.mybank.transfer.service.DuplicateRequestException;
import ru.yandex.practicum.mybank.transfer.service.IdempotencyKeyConflictException;

@RestControllerAdvice
public class ApiExceptionHandler extends BaseExceptionHandler {

	private static final String TRANSACTION_CONFLICT = "transaction_conflict";
	private static final String INTERNAL_ERROR = "internal_error";

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(TransactionRejectedException.class)
	public ResponseEntity<ErrorResponse> handleTransactionRejected(TransactionRejectedException exception) {
		if (TRANSACTION_CONFLICT.equals(exception.getCode())) {
			log.error("Accounts rejected a transaction uuid that was already applied with other details", exception);

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ErrorResponse(INTERNAL_ERROR, "Transfer could not be completed"));
		}

		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
				.body(new ErrorResponse(exception.getCode(), exception.getMessage()));
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
