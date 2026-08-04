package ru.yandex.practicum.mybank.accounts.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mybank.accounts.domain.Account;
import ru.yandex.practicum.mybank.accounts.domain.AggregateType;
import ru.yandex.practicum.mybank.accounts.domain.BalanceOperation;
import ru.yandex.practicum.mybank.accounts.domain.EventType;
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
	private final OutboxService outboxService;

	public TransactionsService(AccountRepository accountRepository, TransactionRepository transactionRepository,
							   BalanceOperationRepository balanceOperationRepository, OutboxService outboxService) {
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.balanceOperationRepository = balanceOperationRepository;
		this.outboxService = outboxService;
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

		TransactionDto dto = toDto(transaction, operation);
		outboxService.save(eventType(dto), AggregateType.TRANSACTION, transaction.getId(), dto);

		return dto;
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

		TransactionDto dto = toDto(transaction, operation);
		outboxService.save(eventType(dto), AggregateType.TRANSACTION, transaction.getId(), dto);

		return dto;
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

		TransactionDto fromDto = toTransferDto(transaction, withdrawal, deposit);
		TransactionDto toDto = toTransferDto(transaction, deposit, withdrawal);

		outboxService.save(eventType(fromDto), AggregateType.TRANSACTION, transaction.getId(), fromDto);
		outboxService.save(eventType(toDto), AggregateType.TRANSACTION, transaction.getId(), toDto);

		return fromDto;
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
		String login = account.getCustomer().getLogin();
		String number = account.getNumber();

		long amount = operation.getAmount();
		long balanceAfter = operation.getBalanceAfter();

		OperationDto cash = switch (operation.getDirection()) {
			case DEPOSIT -> OperationDto.deposit(login, number, amount, balanceAfter);
			case WITHDRAW -> OperationDto.withdrawal(login, number, amount, balanceAfter);
		};

		return new TransactionDto(transaction.getUuid(), transaction.getType(), cash);
	}

	private TransactionDto toTransferDto(Transaction transaction, BalanceOperation ownOperation, BalanceOperation otherOperation) {
		Account ownAccount = ownOperation.getAccount();
		String ownLogin = ownAccount.getCustomer().getLogin();
		String ownNumber = ownAccount.getNumber();

		Account otherAccount = otherOperation.getAccount();
		String otherLogin = otherAccount.getCustomer().getLogin();
		String otherNumber = otherAccount.getNumber();

		long amount = ownOperation.getAmount();
		long balanceAfter = ownOperation.getBalanceAfter();

		OperationDto operation = switch (ownOperation.getDirection()) {
			case WITHDRAW -> OperationDto.sent(ownLogin, ownNumber, otherLogin, otherNumber, amount, balanceAfter);
			case DEPOSIT -> OperationDto.received(otherLogin, otherNumber, ownLogin, ownNumber, amount, balanceAfter);
		};

		return new TransactionDto(transaction.getUuid(), transaction.getType(), operation);
	}

	private EventType eventType(TransactionDto dto) {
		return switch (dto.type()) {
			case DEPOSIT -> EventType.MONEY_DEPOSITED;
			case WITHDRAW -> EventType.MONEY_WITHDRAWN;
			case TRANSFER -> dto.operation().direction() == OperationDirection.WITHDRAW
					? EventType.MONEY_SENT
					: EventType.MONEY_RECEIVED;
		};
	}

	private record TransactionClaim(Transaction transaction, boolean acquired) {
	}
}
