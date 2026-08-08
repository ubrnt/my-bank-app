package ru.yandex.practicum.mybank.transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.mybank.transfer.domain.TransferOperation;

import java.util.Optional;
import java.util.UUID;

public interface TransferOperationRepository extends JpaRepository<TransferOperation, Long> {

	Optional<TransferOperation> findByUuid(UUID uuid);

	@Query(value = """
			select * from transfer_operations
			where uuid = :uuid
			  and (status <> 'PENDING'
			       or updated_ts < now() - (cast(:pendingTimeoutSeconds as double precision) * interval '1 second'))
			""", nativeQuery = true)
	Optional<TransferOperation> findSettledOrExpired(UUID uuid, long pendingTimeoutSeconds);

	@Query(value = """
			insert into transfer_operations (uuid, amount, status, created_ts, updated_ts, version)
			values (:uuid, :amount, 'PENDING', now(), now(), 0)
			on conflict do nothing
			returning *
			""", nativeQuery = true)
	Optional<TransferOperation> insertIfAbsent(UUID uuid, long amount);
}
