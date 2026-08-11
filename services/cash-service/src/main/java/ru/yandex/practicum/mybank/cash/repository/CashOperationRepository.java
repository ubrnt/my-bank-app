package ru.yandex.practicum.mybank.cash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.mybank.cash.domain.CashOperation;

import java.util.Optional;
import java.util.UUID;

public interface CashOperationRepository extends JpaRepository<CashOperation, Long> {

	Optional<CashOperation> findByUuid(UUID uuid);

	@Query(value = """
			update cash_operations
			set status = 'PENDING',
			    failure_reason = null,
			    customer_uuid = null,
			    account_uuid = null,
			    updated_ts = now(),
			    version = version + 1
			where uuid = :uuid
			  and customer_login = :customerLogin
			  and type = :type
			  and amount = :amount
			  and status <> 'COMPLETED'
			  and (status <> 'PENDING'
			       or updated_ts < now() - make_interval(secs => :pendingTimeoutSeconds))
			returning *
			""", nativeQuery = true)
	Optional<CashOperation> reclaimIfSettledOrExpired(UUID uuid, String customerLogin, String type, long amount,
			long pendingTimeoutSeconds);

	@Query(value = """
			insert into cash_operations (uuid, customer_login, type, amount, status, created_ts, updated_ts, version)
			values (:uuid, :customerLogin, :type, :amount, 'PENDING', now(), now(), 0)
			on conflict do nothing
			returning *
			""", nativeQuery = true)
	Optional<CashOperation> insertIfAbsent(UUID uuid, String customerLogin, String type, long amount);
}
