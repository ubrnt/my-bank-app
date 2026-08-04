package ru.yandex.practicum.mybank.accounts.dto;

import ru.yandex.practicum.mybank.accounts.service.dto.CustomerDto;

public record CustomerResponse(
		String login,
		String name
) {

	public static CustomerResponse of(CustomerDto customer) {
		return new CustomerResponse(customer.login(), customer.name());
	}
}
