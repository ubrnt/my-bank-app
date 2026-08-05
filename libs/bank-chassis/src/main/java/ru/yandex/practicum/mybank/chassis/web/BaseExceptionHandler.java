package ru.yandex.practicum.mybank.chassis.web;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

public abstract class BaseExceptionHandler extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
																  @NonNull HttpHeaders headers,
																  @NonNull HttpStatusCode status,
																  @NonNull WebRequest request) {
		List<FieldError> fields = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
				.toList();

		ErrorResponse body = new ErrorResponse("validation_error", "Request validation failed",
				new ValidationErrors(fields));

		return ResponseEntity.status(status).body(body);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(@NonNull Exception exception, Object body,
															 @NonNull HttpHeaders headers,
                                                             HttpStatusCode status,
															 @NonNull WebRequest request) {
		HttpStatus resolved = HttpStatus.resolve(status.value());

		String code = resolved == null ? "error" : resolved.name().toLowerCase();
		String message = resolved == null ? "Request processing failed" : resolved.getReasonPhrase();

		return ResponseEntity.status(status).body(new ErrorResponse(code, message));
	}
}
