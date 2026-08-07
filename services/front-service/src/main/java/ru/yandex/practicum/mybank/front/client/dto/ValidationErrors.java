package ru.yandex.practicum.mybank.front.client.dto;

import java.util.List;

public record ValidationErrors(
		List<FieldError> fields
) {
}
