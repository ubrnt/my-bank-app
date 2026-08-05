package ru.yandex.practicum.mybank.accounts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.mybank.accounts.domain.Customer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

	@Query("select c from Customer c where c.login <> :login order by c.login")
	List<Customer> findOthers(String login);

	Optional<Customer> findByUuid(UUID uuid);
}
