package ru.yandex.practicum.mybank.gateway.web;

public record ErrorResponse(
		String code,
		String message
) {
}
