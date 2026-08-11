package ru.yandex.practicum.mybank.accounts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.mybank.accounts.domain.Transaction;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	Optional<Transaction> findByUuid(UUID uuid);

	@Query(value = """
			insert into transactions (uuid, type, created_ts, updated_ts, version)
			values (:uuid, :type, now(), now(), 0)
			on conflict do nothing
			returning *
			""", nativeQuery = true)
	Optional<Transaction> insertIfAbsent(UUID uuid, String type);
}
