package ru.yandex.practicum.mybank.accounts.domain;

public record CustomerAccount(
		Customer customer,
		Account account
) {
}
