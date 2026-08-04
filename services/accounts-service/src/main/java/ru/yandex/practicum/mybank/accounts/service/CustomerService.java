package ru.yandex.practicum.mybank.accounts.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mybank.accounts.domain.Account;
import ru.yandex.practicum.mybank.accounts.domain.AggregateType;
import ru.yandex.practicum.mybank.accounts.domain.Customer;
import ru.yandex.practicum.mybank.accounts.domain.EventType;
import ru.yandex.practicum.mybank.accounts.domain.CustomerAccount;
import ru.yandex.practicum.mybank.accounts.repository.AccountRepository;
import ru.yandex.practicum.mybank.accounts.repository.CustomerRepository;
import ru.yandex.practicum.mybank.accounts.service.dto.CustomerAccountDto;
import ru.yandex.practicum.mybank.accounts.service.dto.CustomerDto;
import ru.yandex.practicum.mybank.accounts.service.dto.RecipientDto;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final AccountRepository accountRepository;
	private final OutboxService outboxService;

	public CustomerService(CustomerRepository customerRepository, AccountRepository accountRepository,
			OutboxService outboxService) {
		this.customerRepository = customerRepository;
		this.accountRepository = accountRepository;
		this.outboxService = outboxService;
	}

	public CustomerAccountDto getCustomerAccount(String login) {
		return toDto(findCustomerAccount(login));
	}

	public List<CustomerDto> findOthers(String login) {
		return customerRepository.findOthers(login).stream()
				.map(this::toDto)
				.toList();
	}

	@Transactional
	public CustomerAccountDto updateProfile(String login, String name, LocalDate birthdate) {
		CustomerAccount customerAccount = findCustomerAccount(login);

		Customer customer = customerAccount.customer();
		customer.setName(name);
		customer.setBirthdate(birthdate);

		CustomerAccountDto dto = toDto(customerAccount);
		outboxService.save(EventType.PROFILE_UPDATED, AggregateType.CUSTOMER, customer.getId(),
				new RecipientDto(customer.getUuid(), customer.getLogin()), toDto(customer));

		return dto;
	}

	private CustomerAccount findCustomerAccount(String login) {
		return accountRepository.findCustomerAccount(login)
				.orElseThrow(() -> new CustomerAccountNotFoundException(login));
	}

	private CustomerDto toDto(Customer customer) {
		return new CustomerDto(customer.getUuid(), customer.getLogin(), customer.getName());
	}

	//todo ubrnt mappers?
	private CustomerAccountDto toDto(CustomerAccount customerAccount) {
		Customer customer = customerAccount.customer();
		Account account = customerAccount.account();
		return new CustomerAccountDto(
				customer.getLogin(),
				customer.getName(),
				customer.getBirthdate(),
				account.getNumber(),
				account.getBalance());
	}
}
