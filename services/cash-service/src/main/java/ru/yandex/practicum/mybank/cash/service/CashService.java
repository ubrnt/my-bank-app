package ru.yandex.practicum.mybank.cash.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mybank.cash.client.AccountsClient;
import ru.yandex.practicum.mybank.cash.client.TransactionRejectedException;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionRequest;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionResponse;
import ru.yandex.practicum.mybank.cash.domain.CashOperation;
import ru.yandex.practicum.mybank.cash.domain.CashOperationStatus;
import ru.yandex.practicum.mybank.cash.domain.CashOperationType;
import ru.yandex.practicum.mybank.cash.domain.EventType;
import ru.yandex.practicum.mybank.cash.service.dto.CashOperationDto;
import ru.yandex.practicum.mybank.chassis.client.ServiceCallException;

import java.util.UUID;

@Service
public class CashService {

	private static final Logger log = LoggerFactory.getLogger(CashService.class);

	private final AccountsClient accountsClient;
	private final CashOperationJournal journal;

	public CashService(AccountsClient accountsClient, CashOperationJournal journal) {
		this.accountsClient = accountsClient;
		this.journal = journal;
	}

	public CashOperationDto deposit(UUID idempotencyKey, String login, long amount) {
		return process(CashOperationType.DEPOSIT, idempotencyKey, login, amount);
	}

	public CashOperationDto withdraw(UUID idempotencyKey, String login, long amount) {
		return process(CashOperationType.WITHDRAW, idempotencyKey, login, amount);
	}

	private CashOperationDto process(CashOperationType type, UUID idempotencyKey, String login, long amount) {
		CashOperation operation = journal.tryClaim(idempotencyKey, login, type, amount)
				.or(() -> journal.tryReclaim(idempotencyKey, login, type, amount))
				.orElseThrow(() -> new DuplicateRequestException(idempotencyKey));

		if (operation.getStatus() == CashOperationStatus.COMPLETED) {
			return CashOperationDto.of(operation);
		}

		TransactionRequest request = new TransactionRequest(operation.getUuid(), login, amount);

		TransactionResponse transaction;
		try {
			transaction = switch (type) {
				case DEPOSIT -> accountsClient.deposit(request);
				case WITHDRAW -> accountsClient.withdraw(request);
			};
		} catch (TransactionRejectedException e) {
			fail(operation, e.getCode());

			throw e;
		} catch (ServiceCallException e) {
			fail(operation, AccountsServiceUnavailableException.CODE);

			throw new AccountsServiceUnavailableException(e);
		}

		try {
			return CashOperationDto.of(markCompleted(type, operation, transaction));
		} catch (ObjectOptimisticLockingFailureException e) {
			logReclaimed(operation);

			return CashOperationDto.of(operation);
		}
	}

	private CashOperation markCompleted(CashOperationType type, CashOperation operation,
			TransactionResponse transaction) {
		return switch (type) {
			case DEPOSIT -> journal.complete(operation,
					transaction.operation().toAccountUuid(), transaction.operation().toCustomerUuid(),
					EventType.MONEY_DEPOSITED, transaction);
			case WITHDRAW -> journal.complete(operation,
					transaction.operation().fromAccountUuid(), transaction.operation().fromCustomerUuid(),
					EventType.MONEY_WITHDRAWN, transaction);
		};
	}

	private void fail(CashOperation operation, String failureReason) {
		try {
			journal.fail(operation, failureReason);
		} catch (ObjectOptimisticLockingFailureException e) {
			logReclaimed(operation);
		}
	}

	private void logReclaimed(CashOperation operation) {
		log.warn("Operation {} claimed at {} with version {} was reclaimed by a later request, "
						+ "leaving the journal to its owner",
				operation.getUuid(), operation.getUpdatedTs(), operation.getVersion());
	}
}
