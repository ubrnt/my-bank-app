package ru.yandex.practicum.mybank.notifications.dto;

public record ErrorResponse(
		String code,
		String message,
		ValidationErrors validationErrors
) {

	public ErrorResponse(String code, String message) {
		this(code, message, null);
	}
}
