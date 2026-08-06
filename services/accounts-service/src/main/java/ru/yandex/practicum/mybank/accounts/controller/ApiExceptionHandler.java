package ru.yandex.practicum.mybank.accounts.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.mybank.accounts.service.CustomerAccountNotFoundException;
import ru.yandex.practicum.mybank.accounts.service.InsufficientFundsException;
import ru.yandex.practicum.mybank.accounts.service.SameAccountException;
import ru.yandex.practicum.mybank.accounts.service.TransactionConflictException;
import ru.yandex.practicum.mybank.chassis.web.BaseExceptionHandler;
import ru.yandex.practicum.mybank.chassis.web.ErrorResponse;

@RestControllerAdvice
public class ApiExceptionHandler extends BaseExceptionHandler {

	@ExceptionHandler(CustomerAccountNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleCustomerAccountNotFound(CustomerAccountNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ErrorResponse("customer_account_not_found", exception.getMessage()));
	}

	@ExceptionHandler(TransactionConflictException.class)
	public ResponseEntity<ErrorResponse> handleTransactionConflict(TransactionConflictException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ErrorResponse("transaction_conflict", exception.getMessage()));
	}

	@ExceptionHandler(InsufficientFundsException.class)
	public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException exception) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
				.body(new ErrorResponse("insufficient_funds", exception.getMessage()));
	}

	@ExceptionHandler(SameAccountException.class)
	public ResponseEntity<ErrorResponse> handleSameAccount(SameAccountException exception) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
				.body(new ErrorResponse("same_account", exception.getMessage()));
	}
}
