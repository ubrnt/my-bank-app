package ru.yandex.practicum.mybank.accounts.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mybank.accounts.domain.Customer;
import ru.yandex.practicum.mybank.accounts.domain.CustomerAccount;
import ru.yandex.practicum.mybank.accounts.repository.AccountRepository;
import ru.yandex.practicum.mybank.accounts.repository.CustomerRepository;

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

	public CustomerAccount getCustomerAccount(String login) {
		return accountRepository.findCustomerAccount(login)
				.orElseThrow(() -> new CustomerAccountNotFoundException(login));
	}

	public List<Customer> findOthers(String login) {
		return customerRepository.findOthers(login);
	}

	@Transactional
	public CustomerAccount updateProfile(String login, String name, LocalDate birthdate) {
		CustomerAccount customerAccount = getCustomerAccount(login);
		Customer customer = customerAccount.customer();
		customer.setName(name);
		customer.setBirthdate(birthdate);
		return customerAccount;
	}
}
