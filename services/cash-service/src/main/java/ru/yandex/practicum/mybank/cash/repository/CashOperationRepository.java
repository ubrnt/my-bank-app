package ru.yandex.practicum.mybank.cash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.mybank.cash.domain.CashOperation;

import java.util.Optional;
import java.util.UUID;

public interface CashOperationRepository extends JpaRepository<CashOperation, Long> {

	Optional<CashOperation> findByUuid(UUID uuid);
}
