package ru.yandex.practicum.mybank.front.client.dto;

public record FieldError(
		String field,
		String message
) {
}
