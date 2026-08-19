package ru.yandex.practicum.mybank.transfer.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.mybank.chassis.client.ServiceCallException;
import ru.yandex.practicum.mybank.transfer.client.AccountsClient;
import ru.yandex.practicum.mybank.transfer.client.TransactionRejectedException;
import ru.yandex.practicum.mybank.transfer.domain.TransferOperation;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransferServiceTest {

	private static final UUID IDEMPOTENCY_KEY = UUID.fromString("7c9e2b40-5a13-4f8e-9d26-1b0a8c4e0001");

	private final AccountsClient accountsClient = mock(AccountsClient.class);
	private final TransferOperationJournal journal = mock(TransferOperationJournal.class);
	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
	private final TransferService transferService =
			new TransferService(accountsClient, journal, new TransferMetrics(meterRegistry));

	@Test
	void countsRejectedTransferByLoginsAndReason() {
		claimReturns(new TransferOperation(IDEMPOTENCY_KEY, "user1", 500));
		when(accountsClient.transfer(any()))
				.thenThrow(new TransactionRejectedException("insufficient_funds", "Not enough money"));

		assertThatThrownBy(() -> transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500))
				.isInstanceOf(TransactionRejectedException.class);

		assertThat(transferFailures("user1", "user2", "insufficient_funds")).isEqualTo(1.0);
	}

	@Test
	void countsTransferFailedByAccountsOutage() {
		claimReturns(new TransferOperation(IDEMPOTENCY_KEY, "user1", 500));
		when(accountsClient.transfer(any())).thenThrow(new ServiceCallException("accounts-service is down"));

		assertThatThrownBy(() -> transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500))
				.isInstanceOf(AccountsServiceUnavailableException.class);

		assertThat(transferFailures("user1", "user2", AccountsServiceUnavailableException.CODE)).isEqualTo(1.0);
	}

	private void claimReturns(TransferOperation operation) {
		when(journal.tryClaim(IDEMPOTENCY_KEY, "user1", "user2", 500)).thenReturn(Optional.of(operation));
	}

	private double transferFailures(String fromLogin, String toLogin, String reason) {
		return meterRegistry.get(TransferMetrics.TRANSFER_FAILURES)
				.tags("from_login", fromLogin, "to_login", toLogin, "reason", reason)
				.counter()
				.count();
	}
}
