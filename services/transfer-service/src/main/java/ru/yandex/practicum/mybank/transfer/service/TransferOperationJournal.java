package ru.yandex.practicum.mybank.transfer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mybank.notifications.outbox.NotificationsOutboxService;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionOperation;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionResponse;
import ru.yandex.practicum.mybank.transfer.domain.AggregateType;
import ru.yandex.practicum.mybank.transfer.domain.EventType;
import ru.yandex.practicum.mybank.transfer.domain.TransferOperation;
import ru.yandex.practicum.mybank.transfer.domain.TransferOperationStatus;
import ru.yandex.practicum.mybank.transfer.repository.TransferOperationRepository;
import ru.yandex.practicum.mybank.transfer.service.dto.MoneyEventPayloadDto;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class TransferOperationJournal {

	private final TransferOperationRepository transferOperationRepository;
	private final NotificationsOutboxService notificationsOutboxService;
	private final Duration pendingTimeout;

	public TransferOperationJournal(TransferOperationRepository transferOperationRepository,
			NotificationsOutboxService notificationsOutboxService,
			@Value("${mybank.transfer.pending-timeout:30s}") Duration pendingTimeout) {
		this.transferOperationRepository = transferOperationRepository;
		this.notificationsOutboxService = notificationsOutboxService;
		this.pendingTimeout = pendingTimeout;
	}


	@Transactional
	public Optional<TransferOperation> tryClaim(UUID uuid, String fromCustomerLogin, String toCustomerLogin,
			long amount) {
		return transferOperationRepository.insertIfAbsent(uuid, fromCustomerLogin, toCustomerLogin, amount);
	}

	@Transactional
	public Optional<TransferOperation> tryReclaim(UUID uuid, String fromCustomerLogin, String toCustomerLogin,
			long amount) {
		Optional<TransferOperation> reclaimed = transferOperationRepository.reclaimIfSettledOrExpired(uuid,
				fromCustomerLogin, toCustomerLogin, amount, pendingTimeout.toSeconds());

		if (reclaimed.isPresent()) {
			return reclaimed;
		}

		TransferOperation claimed = transferOperationRepository.findByUuid(uuid).orElseThrow();
		if (!claimed.matches(fromCustomerLogin, toCustomerLogin, amount)) {
			throw new IdempotencyKeyConflictException(uuid);
		}

		return Optional.of(claimed).filter(operation -> operation.getStatus() == TransferOperationStatus.COMPLETED);
	}

	@Transactional
	public TransferOperation complete(long operationId, TransactionResponse transaction) {
		TransactionOperation sent = transaction.sent();
		TransactionOperation received = transaction.received();

		TransferOperation operation = transferOperationRepository.findById(operationId).orElseThrow();
		operation.complete(sent.fromAccountUuid(), sent.fromCustomerUuid(),
				received.toAccountUuid(), received.toCustomerUuid());

		save(EventType.MONEY_SENT, operation, sent.fromCustomerUuid(), transaction, sent);
		save(EventType.MONEY_RECEIVED, operation, received.toCustomerUuid(), transaction, received);

		return operation;
	}

	@Transactional
	public TransferOperation fail(long operationId, String failureReason) {
		TransferOperation operation = transferOperationRepository.findById(operationId).orElseThrow();
		operation.fail(failureReason);

		return operation;
	}

	private void save(EventType eventType, TransferOperation operation, UUID recipientUuid,
			TransactionResponse transaction, TransactionOperation leg) {
		notificationsOutboxService.save(eventType.name(), AggregateType.TRANSFER_OPERATION.name(),
				operation.getId(), recipientUuid,
				MoneyEventPayloadDto.of(transaction.uuid(), transaction.type(), leg));
	}
}
