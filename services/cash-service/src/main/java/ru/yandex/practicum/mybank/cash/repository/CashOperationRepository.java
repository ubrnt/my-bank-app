package ru.yandex.practicum.mybank.cash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.mybank.cash.domain.CashOperation;

import java.util.Optional;
import java.util.UUID;

public interface CashOperationRepository extends JpaRepository<CashOperation, Long> {

	Optional<CashOperation> findByUuid(UUID uuid);

	@Query(value = """
			select * from cash_operations
			where uuid = :uuid
			  and (status <> 'PENDING'
			       or updated_ts < now() - (cast(:pendingTimeoutSeconds as double precision) * interval '1 second'))
			""", nativeQuery = true)
	Optional<CashOperation> findSettledOrExpired(UUID uuid, long pendingTimeoutSeconds);

	@Query(value = """
			insert into cash_operations (uuid, type, amount, status, created_ts, updated_ts, version)
			values (:uuid, :type, :amount, 'PENDING', now(), now(), 0)
			on conflict do nothing
			returning *
			""", nativeQuery = true)
	Optional<CashOperation> insertIfAbsent(UUID uuid, String type, long amount);
}
