package ru.yandex.practicum.mybank.accounts.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mybank.accounts.domain.Account;
import ru.yandex.practicum.mybank.accounts.domain.Customer;
import ru.yandex.practicum.mybank.accounts.repository.AccountRepository;
import ru.yandex.practicum.mybank.accounts.repository.CustomerRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AccountService {

	private final CustomerRepository customerRepository;
	private final AccountRepository accountRepository;

	public AccountService(CustomerRepository customerRepository, AccountRepository accountRepository) {
		this.customerRepository = customerRepository;
		this.accountRepository = accountRepository;
	}

	public Account findAccountOf(String login) {
		return accountRepository.findByCustomerLogin(login)
				.orElseThrow(() -> new AccountNotFoundException(login));
	}

	public List<Customer> findOtherCustomers(String login) {
		return customerRepository.findAllByLoginNotOrderByLogin(login);
	}
}
