package ru.yandex.practicum.mybank.transfer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mybank.chassis.client.ServiceCallException;
import ru.yandex.practicum.mybank.transfer.client.AccountsClient;
import ru.yandex.practicum.mybank.transfer.client.TransactionRejectedException;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionRequest;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionResponse;
import ru.yandex.practicum.mybank.transfer.domain.TransferOperation;
import ru.yandex.practicum.mybank.transfer.domain.TransferOperationStatus;
import ru.yandex.practicum.mybank.transfer.service.dto.TransferOperationDto;

import java.util.UUID;

@Service
public class TransferService {

	private static final Logger log = LoggerFactory.getLogger(TransferService.class);

	private final AccountsClient accountsClient;
	private final TransferOperationJournal journal;

	public TransferService(AccountsClient accountsClient, TransferOperationJournal journal) {
		this.accountsClient = accountsClient;
		this.journal = journal;
	}

	public TransferOperationDto transfer(UUID idempotencyKey, String fromLogin, String toLogin, long amount) {
		TransferOperation operation = journal.tryClaim(idempotencyKey, fromLogin, toLogin, amount)
				.or(() -> journal.tryReclaim(idempotencyKey, fromLogin, toLogin, amount))
				.orElseThrow(() -> new DuplicateRequestException(idempotencyKey));

		if (operation.getStatus() == TransferOperationStatus.COMPLETED) {
			return TransferOperationDto.of(operation);
		}

		TransactionResponse transaction;
		try {
			transaction = accountsClient.transfer(
					new TransactionRequest(operation.getUuid(), fromLogin, toLogin, amount));
		} catch (TransactionRejectedException e) {
			fail(operation, e.getCode());

			throw e;
		} catch (ServiceCallException e) {
			fail(operation, AccountsServiceUnavailableException.CODE);

			throw new AccountsServiceUnavailableException(e);
		}

		try {
			return TransferOperationDto.of(journal.complete(operation, transaction));
		} catch (ObjectOptimisticLockingFailureException e) {
			logReclaimed(operation);

			return TransferOperationDto.of(operation);
		}
	}

	private void fail(TransferOperation operation, String failureReason) {
		try {
			journal.fail(operation, failureReason);
		} catch (ObjectOptimisticLockingFailureException e) {
			logReclaimed(operation);
		}
	}

	private void logReclaimed(TransferOperation operation) {
		log.warn("Operation {} claimed at {} with version {} was reclaimed by a later request, "
						+ "leaving the journal to its owner",
				operation.getUuid(), operation.getUpdatedTs(), operation.getVersion());
	}
}
