package ru.yandex.practicum.mybank.accounts.dto;

public record FieldError(
		String field,
		String message
) {
}
