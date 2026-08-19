package ru.yandex.practicum.mybank.cash.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.mybank.cash.client.AccountsClient;
import ru.yandex.practicum.mybank.cash.client.TransactionRejectedException;
import ru.yandex.practicum.mybank.cash.domain.CashOperation;
import ru.yandex.practicum.mybank.cash.domain.CashOperationType;
import ru.yandex.practicum.mybank.chassis.client.ServiceCallException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CashServiceTest {

	private static final UUID IDEMPOTENCY_KEY = UUID.fromString("7c9e2b40-5a13-4f8e-9d26-1b0a8c4e0001");

	private final AccountsClient accountsClient = mock(AccountsClient.class);
	private final CashOperationJournal journal = mock(CashOperationJournal.class);
	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
	private final CashService cashService = new CashService(accountsClient, journal, new CashMetrics(meterRegistry));

	@Test
	void countsRejectedWithdrawalByLoginAndReason() {
		claimReturns(new CashOperation(IDEMPOTENCY_KEY, "user1", CashOperationType.WITHDRAW, 500));
		when(accountsClient.withdraw(any()))
				.thenThrow(new TransactionRejectedException("insufficient_funds", "Not enough money"));

		assertThatThrownBy(() -> cashService.withdraw(IDEMPOTENCY_KEY, "user1", 500))
				.isInstanceOf(TransactionRejectedException.class);

		assertThat(operationFailures("withdraw", "user1", "insufficient_funds")).isEqualTo(1.0);
	}

	@Test
	void countsWithdrawalFailedByAccountsOutage() {
		claimReturns(new CashOperation(IDEMPOTENCY_KEY, "user1", CashOperationType.WITHDRAW, 500));
		when(accountsClient.withdraw(any())).thenThrow(new ServiceCallException("accounts-service is down"));

		assertThatThrownBy(() -> cashService.withdraw(IDEMPOTENCY_KEY, "user1", 500))
				.isInstanceOf(AccountsServiceUnavailableException.class);

		assertThat(operationFailures("withdraw", "user1", AccountsServiceUnavailableException.CODE))
				.isEqualTo(1.0);
	}

	@Test
	void countsRejectedDepositSeparatelyFromWithdrawals() {
		claimReturns(new CashOperation(IDEMPOTENCY_KEY, "user1", CashOperationType.DEPOSIT, 500));
		when(accountsClient.deposit(any()))
				.thenThrow(new TransactionRejectedException("customer_account_not_found", "No account"));

		assertThatThrownBy(() -> cashService.deposit(IDEMPOTENCY_KEY, "user1", 500))
				.isInstanceOf(TransactionRejectedException.class);

		assertThat(operationFailures("deposit", "user1", "customer_account_not_found")).isEqualTo(1.0);
		assertThat(meterRegistry.find(CashMetrics.OPERATION_FAILURES).tags("type", "withdraw").counter())
				.isNull();
	}

	private void claimReturns(CashOperation operation) {
		when(journal.tryClaim(IDEMPOTENCY_KEY, operation.getCustomerLogin(), operation.getType(),
				operation.getAmount())).thenReturn(Optional.of(operation));
	}

	private double operationFailures(String type, String login, String reason) {
		return meterRegistry.get(CashMetrics.OPERATION_FAILURES)
				.tags("type", type, "login", login, "reason", reason)
				.counter()
				.count();
	}
}
