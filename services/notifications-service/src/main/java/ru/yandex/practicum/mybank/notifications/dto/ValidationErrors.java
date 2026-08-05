package ru.yandex.practicum.mybank.notifications.dto;

import java.util.List;

public record ValidationErrors(List<FieldError> fields) {
}
