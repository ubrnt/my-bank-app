package ru.yandex.practicum.mybank.transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.mybank.transfer.domain.TransferOperation;

import java.util.Optional;
import java.util.UUID;

public interface TransferOperationRepository extends JpaRepository<TransferOperation, Long> {

	Optional<TransferOperation> findByTransactionUuid(UUID transactionUuid);
}
