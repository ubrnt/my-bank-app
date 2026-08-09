package ru.yandex.practicum.mybank.cash.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionResponse;
import ru.yandex.practicum.mybank.cash.domain.AggregateType;
import ru.yandex.practicum.mybank.cash.domain.CashOperation;
import ru.yandex.practicum.mybank.cash.domain.CashOperationStatus;
import ru.yandex.practicum.mybank.cash.domain.CashOperationType;
import ru.yandex.practicum.mybank.cash.domain.EventType;
import ru.yandex.practicum.mybank.cash.repository.CashOperationRepository;
import ru.yandex.practicum.mybank.cash.service.dto.MoneyEventPayloadDto;
import ru.yandex.practicum.mybank.notifications.outbox.NotificationsOutboxService;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class CashOperationJournal {

	private final CashOperationRepository cashOperationRepository;
	private final NotificationsOutboxService notificationsOutboxService;
	private final Duration pendingTimeout;

	public CashOperationJournal(CashOperationRepository cashOperationRepository,
			NotificationsOutboxService notificationsOutboxService,
			@Value("${mybank.cash.pending-timeout:30s}") Duration pendingTimeout) {
		this.cashOperationRepository = cashOperationRepository;
		this.notificationsOutboxService = notificationsOutboxService;
		this.pendingTimeout = pendingTimeout;
	}

	@Transactional
	public Optional<CashOperation> tryClaim(UUID uuid, String customerLogin, CashOperationType type,
			long amount) {
		return cashOperationRepository.insertIfAbsent(uuid, customerLogin, type.name(), amount);
	}

	@Transactional
	public Optional<CashOperation> tryReclaim(UUID uuid, String customerLogin, CashOperationType type, long amount) {
		Optional<CashOperation> reclaimed = cashOperationRepository
				.reclaimIfSettledOrExpired(uuid, customerLogin, type.name(), amount, pendingTimeout.toSeconds());

		if (reclaimed.isPresent()) {
			return reclaimed;
		}

		CashOperation claimed = cashOperationRepository.findByUuid(uuid).orElseThrow();
		if (!claimed.matches(customerLogin, type, amount)) {
			throw new IdempotencyKeyConflictException(uuid);
		}

		return Optional.of(claimed).filter(operation -> operation.getStatus() == CashOperationStatus.COMPLETED);
	}

	@Transactional
	public CashOperation complete(CashOperation claimed, UUID accountUuid, UUID customerUuid,
			EventType eventType, TransactionResponse transaction) {
		claimed.complete(accountUuid, customerUuid);
		CashOperation operation = cashOperationRepository.save(claimed);

		notificationsOutboxService.save(eventType.name(), AggregateType.CASH_OPERATION.name(),
				operation.getId(), customerUuid, MoneyEventPayloadDto.of(transaction));

		return operation;
	}

	@Transactional
	public CashOperation fail(CashOperation claimed, String failureReason) {
		claimed.fail(failureReason);

		return cashOperationRepository.save(claimed);
	}
}
