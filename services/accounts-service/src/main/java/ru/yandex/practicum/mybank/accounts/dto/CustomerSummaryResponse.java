package ru.yandex.practicum.mybank.accounts.dto;

import ru.yandex.practicum.mybank.accounts.domain.Customer;

public record CustomerSummaryResponse(
		String login,
		String name
) {

	public static CustomerSummaryResponse of(Customer customer) {
		return new CustomerSummaryResponse(customer.getLogin(), customer.getName());
	}
}
