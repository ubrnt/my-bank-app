package ru.yandex.practicum.mybank.front.client.dto;

public record ErrorResponse(
		String code,
		String message,
		ValidationErrors validationErrors
) {
}
