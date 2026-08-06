package ru.yandex.practicum.mybank.transfer.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.mybank.chassis.client.ServiceCallException;
import ru.yandex.practicum.mybank.transfer.client.AccountsClient;
import ru.yandex.practicum.mybank.transfer.client.TransactionRejectedException;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionRequest;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionResponse;
import ru.yandex.practicum.mybank.transfer.service.dto.TransferOperationDto;

import java.util.UUID;

@Service
public class TransferService {

	private static final String ACCOUNTS_UNAVAILABLE = "accounts_unavailable";

	private final AccountsClient accountsClient;
	private final TransferOperationJournal journal;

	public TransferService(AccountsClient accountsClient, TransferOperationJournal journal) {
		this.accountsClient = accountsClient;
		this.journal = journal;
	}

	public TransferOperationDto transfer(String fromLogin, String toLogin, long amount) {
		UUID transactionUuid = UUID.randomUUID();

		TransactionResponse transaction;
		try {
			transaction = accountsClient.transfer(new TransactionRequest(transactionUuid, fromLogin, toLogin, amount));
		} catch (TransactionRejectedException e) {
			journal.fail(transactionUuid, amount, e.getCode());

			throw e;
		} catch (ServiceCallException e) {
			journal.fail(transactionUuid, amount, ACCOUNTS_UNAVAILABLE);

			throw new AccountsServiceUnavailableException(e);
		}

		return TransferOperationDto.of(journal.complete(transactionUuid, amount, transaction));
	}
}
