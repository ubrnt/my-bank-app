package ru.yandex.practicum.mybank.accounts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.mybank.accounts.domain.Account;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

	@Query("select a from Account a join fetch a.customer c where c.login = :login")
	Optional<Account> findByCustomerLogin(String login);
}
