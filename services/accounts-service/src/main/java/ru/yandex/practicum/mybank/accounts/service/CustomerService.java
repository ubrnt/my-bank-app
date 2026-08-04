package ru.yandex.practicum.mybank.accounts.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mybank.accounts.domain.Account;
import ru.yandex.practicum.mybank.accounts.domain.Customer;
import ru.yandex.practicum.mybank.accounts.domain.CustomerAccount;
import ru.yandex.practicum.mybank.accounts.repository.AccountRepository;
import ru.yandex.practicum.mybank.accounts.repository.CustomerRepository;
import ru.yandex.practicum.mybank.accounts.service.dto.CustomerAccountDto;
import ru.yandex.practicum.mybank.accounts.service.dto.CustomerDto;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final AccountRepository accountRepository;

	public CustomerService(CustomerRepository customerRepository, AccountRepository accountRepository) {
		this.customerRepository = customerRepository;
		this.accountRepository = accountRepository;
	}

	public CustomerAccountDto getCustomerAccount(String login) {
		return customerAccount(findCustomerAccount(login));
	}

	public List<CustomerDto> findOthers(String login) {
		return customerRepository.findOthers(login).stream()
				.map(customer -> new CustomerDto(customer.getLogin(), customer.getName()))
				.toList();
	}

	@Transactional
	public CustomerAccountDto updateProfile(String login, String name, LocalDate birthdate) {
		CustomerAccount customerAccount = findCustomerAccount(login);

		Customer customer = customerAccount.customer();
		customer.setName(name);
		customer.setBirthdate(birthdate);

		return customerAccount(customerAccount);
	}

	private CustomerAccount findCustomerAccount(String login) {
		return accountRepository.findCustomerAccount(login)
				.orElseThrow(() -> new CustomerAccountNotFoundException(login));
	}

	private CustomerAccountDto customerAccount(CustomerAccount customerAccount) {
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
