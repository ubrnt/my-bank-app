package ru.yandex.practicum.mybank.transfer.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mybank.notifications.outbox.NotificationsOutboxService;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionOperation;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionResponse;
import ru.yandex.practicum.mybank.transfer.domain.AggregateType;
import ru.yandex.practicum.mybank.transfer.domain.EventType;
import ru.yandex.practicum.mybank.transfer.domain.TransferOperation;
import ru.yandex.practicum.mybank.transfer.repository.TransferOperationRepository;
import ru.yandex.practicum.mybank.transfer.service.dto.MoneyEventPayloadDto;

import java.util.UUID;

@Component
public class TransferOperationJournal {

	private final TransferOperationRepository transferOperationRepository;
	private final NotificationsOutboxService notificationsOutboxService;

	public TransferOperationJournal(TransferOperationRepository transferOperationRepository,
			NotificationsOutboxService notificationsOutboxService) {
		this.transferOperationRepository = transferOperationRepository;
		this.notificationsOutboxService = notificationsOutboxService;
	}

	@Transactional
	public TransferOperation complete(UUID transactionUuid, long amount, TransactionResponse transaction) {
		TransactionOperation sent = transaction.sent();
		TransactionOperation received = transaction.received();

		TransferOperation operation = transferOperationRepository.save(TransferOperation.completed(
				transactionUuid, amount,
				sent.fromAccountUuid(), sent.fromCustomerUuid(),
				received.toAccountUuid(), received.toCustomerUuid()));

		save(EventType.MONEY_SENT, operation, sent.fromCustomerUuid(), transaction, sent);
		save(EventType.MONEY_RECEIVED, operation, received.toCustomerUuid(), transaction, received);

		return operation;
	}

	@Transactional
	public TransferOperation fail(UUID transactionUuid, long amount, String failureReason) {
		return transferOperationRepository.save(TransferOperation.failed(transactionUuid, amount, failureReason));
	}

	private void save(EventType eventType, TransferOperation operation, UUID recipientUuid,
			TransactionResponse transaction, TransactionOperation leg) {
		notificationsOutboxService.save(eventType.name(), AggregateType.TRANSFER_OPERATION.name(),
				operation.getId(), recipientUuid,
				MoneyEventPayloadDto.of(transaction.uuid(), transaction.type(), leg));
	}
}
