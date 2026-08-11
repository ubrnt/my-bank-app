package ru.yandex.practicum.mybank.accounts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.practicum.mybank.accounts.domain.Account;
import ru.yandex.practicum.mybank.accounts.domain.BalanceOperation;
import ru.yandex.practicum.mybank.accounts.domain.Customer;
import ru.yandex.practicum.mybank.accounts.domain.OperationDirection;
import ru.yandex.practicum.mybank.accounts.domain.Transaction;
import ru.yandex.practicum.mybank.accounts.domain.TransactionType;
import ru.yandex.practicum.mybank.accounts.repository.AccountRepository;
import ru.yandex.practicum.mybank.accounts.repository.BalanceOperationRepository;
import ru.yandex.practicum.mybank.accounts.repository.TransactionRepository;
import ru.yandex.practicum.mybank.accounts.service.dto.TransactionDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransactionsServiceTest {

	private static final UUID TRANSACTION_UUID = UUID.fromString("cccc0001-2222-4333-8444-555566660001");
	private static final long INITIAL_BALANCE = 1000;

	private final AccountRepository accountRepository = mock(AccountRepository.class);
	private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
	private final BalanceOperationRepository balanceOperationRepository = mock(BalanceOperationRepository.class);

	private final TransactionsService transactionsService = new TransactionsService(
			accountRepository, transactionRepository, balanceOperationRepository);

	private Account senderAccount;
	private Account recipientAccount;

	@BeforeEach
	void setUp() {
		senderAccount = account("user1", "40817810000000000001");
		recipientAccount = account("user2", "40817810000000000002");

		when(accountRepository.findForUpdateByLogin("user1")).thenReturn(Optional.of(senderAccount));
		when(accountRepository.findForUpdateByLogin("user2")).thenReturn(Optional.of(recipientAccount));
		when(balanceOperationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void rejectsTransferToTheSameAccount() {
		assertThatThrownBy(() -> transactionsService.transfer(TRANSACTION_UUID, "user1", "user1", 100))
				.isInstanceOf(SameAccountException.class);

		verifyNoInteractions(transactionRepository, accountRepository, balanceOperationRepository);
	}

	@Test
	void rejectsWithdrawalBeyondBalance() {
		claimAcquired(TransactionType.WITHDRAW);

		assertThatThrownBy(() -> transactionsService.withdraw(TRANSACTION_UUID, "user1", INITIAL_BALANCE + 1))
				.isInstanceOf(InsufficientFundsException.class);

		assertThat(senderAccount.getBalance()).isEqualTo(INITIAL_BALANCE);
		verify(balanceOperationRepository, never()).save(any());
	}

	@Test
	void rejectsDepositBeyondBalanceLimit() {
		claimAcquired(TransactionType.DEPOSIT);
		ReflectionTestUtils.setField(senderAccount, "balance", Long.MAX_VALUE - 10);

		assertThatThrownBy(() -> transactionsService.deposit(TRANSACTION_UUID, "user1", 11))
				.isInstanceOf(BalanceLimitExceededException.class);

		assertThat(senderAccount.getBalance()).isEqualTo(Long.MAX_VALUE - 10);
		verify(balanceOperationRepository, never()).save(any());
	}

	@Test
	void rejectsTransferBeyondRecipientBalanceLimit() {
		claimAcquired(TransactionType.TRANSFER);
		ReflectionTestUtils.setField(recipientAccount, "balance", Long.MAX_VALUE - 10);

		assertThatThrownBy(() -> transactionsService.transfer(TRANSACTION_UUID, "user1", "user2", 11))
				.isInstanceOf(BalanceLimitExceededException.class);

		assertThat(senderAccount.getBalance()).isEqualTo(INITIAL_BALANCE);
		assertThat(recipientAccount.getBalance()).isEqualTo(Long.MAX_VALUE - 10);
		verify(balanceOperationRepository, never()).save(any());
	}

	@Test
	void locksAccountsInLoginOrder() {
		claimAcquired(TransactionType.TRANSFER);

		transactionsService.transfer(TRANSACTION_UUID, "user2", "user1", 100);

		InOrder order = inOrder(accountRepository);
		order.verify(accountRepository).findForUpdateByLogin("user1");
		order.verify(accountRepository).findForUpdateByLogin("user2");
	}

	@Test
	void repeatedCallReturnsTheStoredResult() {
		Transaction applied = claimLost(TransactionType.DEPOSIT);
		when(balanceOperationRepository.findMatchingOperations(applied, OperationDirection.DEPOSIT, "user1", 100))
				.thenReturn(List.of(new BalanceOperation(applied, senderAccount, OperationDirection.DEPOSIT, 100)));

		TransactionDto result = transactionsService.deposit(TRANSACTION_UUID, "user1", 100);

		assertThat(result.uuid()).isEqualTo(TRANSACTION_UUID);
		assertThat(senderAccount.getBalance()).isEqualTo(INITIAL_BALANCE);
		verify(accountRepository, never()).findForUpdateByLogin(any());
		verify(balanceOperationRepository, never()).save(any());
	}

	@Test
	void repeatedCallWithOtherDetailsCausesConflict() {
		Transaction applied = claimLost(TransactionType.DEPOSIT);
		when(balanceOperationRepository.findMatchingOperations(applied, OperationDirection.DEPOSIT, "user1", 100))
				.thenReturn(List.of());

		assertThatThrownBy(() -> transactionsService.deposit(TRANSACTION_UUID, "user1", 100))
				.isInstanceOf(TransactionConflictException.class);
	}

	@Test
	void repeatedCallOfAnotherTypeCausesConflict() {
		claimLost(TransactionType.WITHDRAW);

		assertThatThrownBy(() -> transactionsService.deposit(TRANSACTION_UUID, "user1", 100))
				.isInstanceOf(TransactionConflictException.class);
	}

	private void claimAcquired(TransactionType type) {
		when(transactionRepository.insertIfAbsent(TRANSACTION_UUID, type.name()))
				.thenReturn(Optional.of(new Transaction(TRANSACTION_UUID, type)));
	}

	private Transaction claimLost(TransactionType applied) {
		Transaction transaction = new Transaction(TRANSACTION_UUID, applied);

		when(transactionRepository.insertIfAbsent(any(), any())).thenReturn(Optional.empty());
		when(transactionRepository.findByUuid(TRANSACTION_UUID)).thenReturn(Optional.of(transaction));

		return transaction;
	}

	private static Account account(String login, String number) {
		Customer customer = new Customer(login, "Кто-то " + login, LocalDate.of(1990, 1, 15));
		Account account = new Account(number, customer);
		ReflectionTestUtils.setField(account, "balance", INITIAL_BALANCE);

		return account;
	}
}
