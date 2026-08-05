package ru.yandex.practicum.mybank.notifications.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.yandex.practicum.mybank.notifications.client.CustomerResolutionException;
import ru.yandex.practicum.mybank.notifications.dto.ErrorResponse;
import ru.yandex.practicum.mybank.notifications.dto.FieldError;
import ru.yandex.practicum.mybank.notifications.dto.ValidationErrors;
import ru.yandex.practicum.mybank.notifications.service.InvalidEventException;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

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

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldError> fields = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
				.toList();
		ErrorResponse body = new ErrorResponse("validation_error", "Request validation failed",
				new ValidationErrors(fields));
		return ResponseEntity.status(status).body(body);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		HttpStatus resolved = HttpStatus.resolve(status.value());
		String code = resolved == null ? "error" : resolved.name().toLowerCase();
		String message = resolved == null ? "Request processing failed" : resolved.getReasonPhrase();
		return ResponseEntity.status(status).body(new ErrorResponse(code, message));
	}
}
