package ru.yandex.practicum.mybank.cash.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.mybank.cash.client.AccountsClient;
import ru.yandex.practicum.mybank.cash.client.TransactionRejectedException;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionRequest;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionResponse;
import ru.yandex.practicum.mybank.cash.domain.CashOperation;
import ru.yandex.practicum.mybank.cash.domain.CashOperationType;
import ru.yandex.practicum.mybank.cash.domain.EventType;
import ru.yandex.practicum.mybank.cash.service.dto.CashOperationDto;
import ru.yandex.practicum.mybank.chassis.client.ServiceCallException;

@Service
public class CashService {

	private static final String ACCOUNTS_UNAVAILABLE = "accounts_unavailable";

	private final AccountsClient accountsClient;
	private final CashOperationJournal journal;

	public CashService(AccountsClient accountsClient, CashOperationJournal journal) {
		this.accountsClient = accountsClient;
		this.journal = journal;
	}

	public CashOperationDto deposit(String login, long amount) {
		return process(CashOperationType.DEPOSIT, login, amount);
	}

	public CashOperationDto withdraw(String login, long amount) {
		return process(CashOperationType.WITHDRAW, login, amount);
	}

	private CashOperationDto process(CashOperationType type, String login, long amount) {
		CashOperation operation = journal.pending(type, amount);

		TransactionRequest request = new TransactionRequest(operation.getUuid(), login, amount);

		TransactionResponse transaction;
		try {
			transaction = switch (type) {
				case DEPOSIT -> accountsClient.deposit(request);
				case WITHDRAW -> accountsClient.withdraw(request);
			};
		} catch (TransactionRejectedException e) {
			journal.fail(operation.getId(), e.getCode());

			throw e;
		} catch (ServiceCallException e) {
			journal.fail(operation.getId(), ACCOUNTS_UNAVAILABLE);

			throw new AccountsServiceUnavailableException(e);
		}

		return CashOperationDto.of(finalizeCompleted(type, operation.getId(), transaction));
	}

	private CashOperation finalizeCompleted(CashOperationType type, long operationId, TransactionResponse transaction) {
		return switch (type) {
			case DEPOSIT -> journal.complete(operationId,
					transaction.operation().toAccountUuid(), transaction.operation().toCustomerUuid(),
					EventType.MONEY_DEPOSITED, transaction);
			case WITHDRAW -> journal.complete(operationId,
					transaction.operation().fromAccountUuid(), transaction.operation().fromCustomerUuid(),
					EventType.MONEY_WITHDRAWN, transaction);
		};
	}
}
