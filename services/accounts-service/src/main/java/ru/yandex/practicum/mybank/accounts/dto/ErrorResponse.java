package ru.yandex.practicum.mybank.accounts.dto;

public record ErrorResponse(
		String code,
		String message,
		ValidationErrors validationErrors
) {

	public ErrorResponse(String code, String message) {
		this(code, message, null);
	}
}
