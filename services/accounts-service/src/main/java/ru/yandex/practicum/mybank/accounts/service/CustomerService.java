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
import ru.yandex.practicum.mybank.accounts.service.dto.CustomerUpdatedPayloadDto;
import ru.yandex.practicum.mybank.notifications.outbox.NotificationsOutboxService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final AccountRepository accountRepository;
	private final NotificationsOutboxService notificationsOutboxService;

	public CustomerService(CustomerRepository customerRepository, AccountRepository accountRepository,
			NotificationsOutboxService notificationsOutboxService) {
		this.customerRepository = customerRepository;
		this.accountRepository = accountRepository;
		this.notificationsOutboxService = notificationsOutboxService;
	}

	public CustomerAccountDto getCustomerAccount(String login) {
		return toDto(loadCustomerAccount(login));
	}

	public List<CustomerDto> findOthers(String login) {
		return customerRepository.findOthers(login).stream()
				.map(this::toDto)
				.toList();
	}

	public CustomerDto getCustomer(UUID uuid) {
		return customerRepository.findByUuid(uuid)
				.map(this::toDto)
				.orElseThrow(() -> new CustomerAccountNotFoundException(uuid));
	}

	@Transactional
	public CustomerAccountDto updateProfile(String login, String name, LocalDate birthdate) {
		CustomerAccount customerAccount = loadCustomerAccount(login);

		Customer customer = customerAccount.customer();
		boolean changed = !name.equals(customer.getName()) || !birthdate.equals(customer.getBirthdate());

		customer.setName(name);
		customer.setBirthdate(birthdate);

		if (changed) {
			notificationsOutboxService.save(EventType.CUSTOMER_UPDATED.name(), AggregateType.CUSTOMER.name(),
					customer.getId(), customer.getUuid(), new CustomerUpdatedPayloadDto(customer.getUuid()));
		}

		return toDto(customerAccount);
	}

	private CustomerAccount loadCustomerAccount(String login) {
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
