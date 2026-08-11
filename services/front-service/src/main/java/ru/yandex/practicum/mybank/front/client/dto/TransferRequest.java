package ru.yandex.practicum.mybank.front.client.dto;

public record TransferRequest(
		String toLogin,
		long amount
) {
}
