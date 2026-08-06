package ru.yandex.practicum.mybank.accounts.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mybank.accounts.domain.Account;
import ru.yandex.practicum.mybank.accounts.domain.BalanceOperation;
import ru.yandex.practicum.mybank.accounts.domain.OperationDirection;
import ru.yandex.practicum.mybank.accounts.domain.Transaction;
import ru.yandex.practicum.mybank.accounts.domain.TransactionType;
import ru.yandex.practicum.mybank.accounts.repository.AccountRepository;
import ru.yandex.practicum.mybank.accounts.repository.BalanceOperationRepository;
import ru.yandex.practicum.mybank.accounts.repository.TransactionRepository;
import ru.yandex.practicum.mybank.accounts.service.dto.OperationDto;
import ru.yandex.practicum.mybank.accounts.service.dto.TransactionDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TransactionsService {

	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;
	private final BalanceOperationRepository balanceOperationRepository;

	public TransactionsService(AccountRepository accountRepository, TransactionRepository transactionRepository,
							   BalanceOperationRepository balanceOperationRepository) {
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.balanceOperationRepository = balanceOperationRepository;
	}

	public TransactionDto deposit(UUID transactionUuid, String login, long amount) {
		TransactionClaim claim = tryAcquireClaim(transactionUuid, TransactionType.DEPOSIT);
		Transaction transaction = claim.transaction();

		if (!claim.acquired()) {
			List<BalanceOperation> matched = balanceOperationRepository.findMatchingOperations(
					transaction, OperationDirection.DEPOSIT, login, amount);
			requireOperationCount(transaction, matched);

			return toDto(transaction, matched.getFirst());
		}

		Account account = loadWithLock(login);
		account.deposit(amount);

		BalanceOperation operation = balanceOperationRepository
				.save(new BalanceOperation(transaction, account, OperationDirection.DEPOSIT, amount));

		return toDto(transaction, operation);
	}

	public TransactionDto withdraw(UUID transactionUuid, String login, long amount) {
		TransactionClaim claim = tryAcquireClaim(transactionUuid, TransactionType.WITHDRAW);
		Transaction transaction = claim.transaction();

		if (!claim.acquired()) {
			List<BalanceOperation> matched = balanceOperationRepository.findMatchingOperations(
					transaction, OperationDirection.WITHDRAW, login, amount);
			requireOperationCount(transaction, matched);

			return toDto(transaction, matched.getFirst());
		}

		Account account = loadWithLock(login);
		requireSufficientFunds(login, account, amount);
		account.withdraw(amount);

		BalanceOperation operation = balanceOperationRepository
				.save(new BalanceOperation(transaction, account, OperationDirection.WITHDRAW, amount));

		return toDto(transaction, operation);
	}

	public TransactionDto transfer(UUID transactionUuid, String fromLogin, String toLogin, long amount) {
		if (fromLogin.equals(toLogin)) {
			throw new SameAccountException(fromLogin);
		}

		TransactionClaim claim = tryAcquireClaim(transactionUuid, TransactionType.TRANSFER);
		Transaction transaction = claim.transaction();

		if (!claim.acquired()) {
			List<BalanceOperation> matched = balanceOperationRepository.findMatchingTransferOperations(
					transaction, OperationDirection.WITHDRAW, fromLogin, OperationDirection.DEPOSIT, toLogin, amount);
			requireOperationCount(transaction, matched);

			return toTransferDto(transaction, operation(matched, OperationDirection.WITHDRAW),
					operation(matched, OperationDirection.DEPOSIT));
		}

		Account from;
		Account to;
		if (fromLogin.compareTo(toLogin) < 0) {
			from = loadWithLock(fromLogin);
			to = loadWithLock(toLogin);
		} else {
			to = loadWithLock(toLogin);
			from = loadWithLock(fromLogin);
		}
		requireSufficientFunds(fromLogin, from, amount);
		from.withdraw(amount);
		to.deposit(amount);

		BalanceOperation withdrawal = balanceOperationRepository
				.save(new BalanceOperation(transaction, from, OperationDirection.WITHDRAW, amount));
		BalanceOperation deposit = balanceOperationRepository
				.save(new BalanceOperation(transaction, to, OperationDirection.DEPOSIT, amount));

		return toTransferDto(transaction, withdrawal, deposit);
	}

	private TransactionClaim tryAcquireClaim(UUID transactionUuid, TransactionType type) {
		Optional<Transaction> claimed = transactionRepository.insertIfAbsent(transactionUuid, type.name());
		if (claimed.isPresent()) {
			return new TransactionClaim(claimed.get(), true);
		}

		Transaction applied = transactionRepository.findByUuid(transactionUuid).orElseThrow();
		if (applied.getType() != type) {
			throw new TransactionConflictException(transactionUuid);
		}

		return new TransactionClaim(applied, false);
	}

	private void requireSufficientFunds(String login, Account account, long amount) {
		if (account.getBalance() < amount) {
			throw new InsufficientFundsException(login, amount, account.getBalance());
		}
	}

	private Account loadWithLock(String login) {
		return accountRepository.findForUpdateByLogin(login)
				.orElseThrow(() -> new CustomerAccountNotFoundException(login));
	}

	private BalanceOperation operation(List<BalanceOperation> operations, OperationDirection direction) {
		return operations.stream()
				.filter(operation -> operation.getDirection() == direction)
				.findFirst()
				.orElseThrow();
	}

	private void requireOperationCount(Transaction transaction, List<BalanceOperation> matchedOperations) {
		if (matchedOperations.size() != transaction.getType().operationCount()) {
			throw new TransactionConflictException(transaction.getUuid());
		}
	}

	//todo mappers ubrnt?
	private TransactionDto toDto(Transaction transaction, BalanceOperation operation) {
		Account account = operation.getAccount();
		long amount = operation.getAmount();
		long balanceAfter = operation.getBalanceAfter();

		OperationDto cash = switch (operation.getDirection()) {
			case DEPOSIT -> OperationDto.deposit(account, amount, balanceAfter);
			case WITHDRAW -> OperationDto.withdrawal(account, amount, balanceAfter);
		};

		return new TransactionDto(transaction.getUuid(), transaction.getType(), List.of(cash));
	}

	private TransactionDto toTransferDto(Transaction transaction, BalanceOperation withdrawal, BalanceOperation deposit) {
		Account from = withdrawal.getAccount();
		Account to = deposit.getAccount();
		long amount = withdrawal.getAmount();

		OperationDto sent = OperationDto.sent(from, to, amount, withdrawal.getBalanceAfter());
		OperationDto received = OperationDto.received(from, to, amount, deposit.getBalanceAfter());

		return new TransactionDto(transaction.getUuid(), transaction.getType(), List.of(sent, received));
	}

	private record TransactionClaim(Transaction transaction, boolean acquired) {
	}
}
