package ru.yandex.practicum.mybank.chassis.web;

public record FieldError(
		String field,
		String message
) {
}
