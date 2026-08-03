package ru.yandex.practicum.mybank.accounts.dto;

import java.util.List;

public record ValidationErrors(
		List<FieldError> fields
) {
}
