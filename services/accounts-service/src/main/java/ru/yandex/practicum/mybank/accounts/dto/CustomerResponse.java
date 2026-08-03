package ru.yandex.practicum.mybank.accounts.dto;

import ru.yandex.practicum.mybank.accounts.domain.Customer;

public record CustomerResponse(
		String login,
		String name
) {

	public static CustomerResponse of(Customer customer) {
		return new CustomerResponse(customer.getLogin(), customer.getName());
	}
}
