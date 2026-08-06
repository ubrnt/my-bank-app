package ru.yandex.practicum.mybank.notifications.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.mybank.chassis.web.BaseExceptionHandler;
import ru.yandex.practicum.mybank.chassis.web.ErrorResponse;
import ru.yandex.practicum.mybank.notifications.client.CustomerResolutionException;
import ru.yandex.practicum.mybank.notifications.service.InvalidEventException;

@RestControllerAdvice
public class ApiExceptionHandler extends BaseExceptionHandler {

	@ExceptionHandler(InvalidEventException.class)
	public ResponseEntity<ErrorResponse> handleInvalidEvent(InvalidEventException exception) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
				.body(new ErrorResponse("invalid_event", exception.getMessage()));
	}

	@ExceptionHandler(CustomerResolutionException.class)
	public ResponseEntity<ErrorResponse> handleCustomerResolution(CustomerResolutionException exception) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ErrorResponse("recipient_resolution_failed", exception.getMessage()));
	}
}
