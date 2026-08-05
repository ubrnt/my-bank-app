package ru.yandex.practicum.mybank.chassis.web;

import java.util.List;

public record ValidationErrors(
		List<FieldError> fields
) {
}
