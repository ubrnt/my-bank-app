package ru.yandex.practicum.mybank.accounts.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.yandex.practicum.mybank.accounts.dto.ErrorResponse;
import ru.yandex.practicum.mybank.accounts.dto.FieldError;
import ru.yandex.practicum.mybank.accounts.dto.ValidationErrors;
import ru.yandex.practicum.mybank.accounts.service.CustomerAccountNotFoundException;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(CustomerAccountNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleCustomerAccountNotFound(CustomerAccountNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ErrorResponse("customer_account_not_found", exception.getMessage()));
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldError> fields = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
				.toList();
		ErrorResponse body = new ErrorResponse("validation_error", "Проверьте заполнение полей",
				new ValidationErrors(fields));
		return ResponseEntity.status(status).body(body);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		HttpStatus resolved = HttpStatus.resolve(status.value());
		String code = resolved == null ? "error" : resolved.name().toLowerCase();
		String message = resolved == null ? "Ошибка обработки запроса" : resolved.getReasonPhrase();
		return ResponseEntity.status(status).body(new ErrorResponse(code, message));
	}
}
