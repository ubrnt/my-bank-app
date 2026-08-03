package ru.yandex.practicum.mybank.accounts.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
		String code,
		String message,
		ValidationErrors validationErrors
) {

	public ErrorResponse(String code, String message) {
		this(code, message, null);
	}
}
