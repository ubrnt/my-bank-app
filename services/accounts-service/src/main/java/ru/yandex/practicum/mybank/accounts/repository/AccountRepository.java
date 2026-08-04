package ru.yandex.practicum.mybank.accounts.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select a
			from Account a
			join fetch a.customer c
			where c.login = :login
			""")
	Optional<Account> findForUpdateByLogin(String login);
}
