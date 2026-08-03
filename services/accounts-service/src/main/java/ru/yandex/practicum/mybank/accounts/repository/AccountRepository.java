package ru.yandex.practicum.mybank.accounts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.mybank.accounts.domain.Account;
import ru.yandex.practicum.mybank.accounts.domain.CustomerAccount;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

	@Query("""
			select new ru.yandex.practicum.mybank.accounts.domain.CustomerAccount(c, a)
			from Account a
			join a.customer c
			where c.login = :login
			""")
	Optional<CustomerAccount> findCustomerAccount(String login);
}
