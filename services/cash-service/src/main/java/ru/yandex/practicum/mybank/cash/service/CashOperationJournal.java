package ru.yandex.practicum.mybank.cash.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionResponse;
import ru.yandex.practicum.mybank.cash.domain.AggregateType;
import ru.yandex.practicum.mybank.cash.domain.CashOperation;
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

	public Optional<CashOperation> findSettledOrExpired(UUID uuid) {
		return cashOperationRepository.findSettledOrExpired(uuid, pendingTimeout.toSeconds());
	}

	@Transactional
	public Optional<CashOperation> tryAcquireClaim(UUID uuid, CashOperationType type, long amount) {
		return cashOperationRepository.insertIfAbsent(uuid, type.name(), amount);
	}

	@Transactional
	public CashOperation complete(long operationId, UUID accountUuid, UUID customerUuid,
			EventType eventType, TransactionResponse transaction) {
		CashOperation operation = cashOperationRepository.findById(operationId).orElseThrow();
		operation.complete(accountUuid, customerUuid);

		notificationsOutboxService.save(eventType.name(), AggregateType.CASH_OPERATION.name(),
				operation.getId(), customerUuid, MoneyEventPayloadDto.of(transaction));

		return operation;
	}

	@Transactional
	public CashOperation fail(long operationId, String failureReason) {
		CashOperation operation = cashOperationRepository.findById(operationId).orElseThrow();
		operation.fail(failureReason);

		return operation;
	}
}
