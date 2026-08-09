package ru.yandex.practicum.mybank.transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.mybank.transfer.domain.TransferOperation;

import java.util.Optional;
import java.util.UUID;

public interface TransferOperationRepository extends JpaRepository<TransferOperation, Long> {

	Optional<TransferOperation> findByUuid(UUID uuid);

	@Query(value = """
			update transfer_operations
			set status = 'PENDING',
			    failure_reason = null,
			    from_customer_uuid = null,
			    from_account_uuid = null,
			    to_customer_uuid = null,
			    to_account_uuid = null,
			    updated_ts = now(),
			    version = version + 1
			where uuid = :uuid
			  and from_customer_login = :fromCustomerLogin
			  and amount = :amount
			  and status <> 'COMPLETED'
			  and (status <> 'PENDING'
			       or updated_ts < now() - make_interval(secs => :pendingTimeoutSeconds))
			returning *
			""", nativeQuery = true)
	Optional<TransferOperation> reclaimIfSettledOrExpired(UUID uuid, String fromCustomerLogin, long amount,
			long pendingTimeoutSeconds);

	@Query(value = """
			insert into transfer_operations (uuid, from_customer_login, amount, status, created_ts, updated_ts, version)
			values (:uuid, :fromCustomerLogin, :amount, 'PENDING', now(), now(), 0)
			on conflict do nothing
			returning *
			""", nativeQuery = true)
	Optional<TransferOperation> insertIfAbsent(UUID uuid, String fromCustomerLogin, long amount);
}
