package ru.yandex.practicum.mybank.accounts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "balance_operations")
public class BalanceOperation extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "transaction_id", nullable = false, updatable = false)
	private Transaction transaction;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false, updatable = false)
	private Account account;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 16)
	private OperationDirection direction;

	@Column(nullable = false, updatable = false)
	private long amount;

	@Column(name = "balance_after", nullable = false, updatable = false)
	private long balanceAfter;

	protected BalanceOperation() {
	}

	public BalanceOperation(Transaction transaction, Account account, OperationDirection direction, long amount) {
		this.transaction = transaction;
		this.account = account;
		this.direction = direction;
		this.amount = amount;
		this.balanceAfter = account.getBalance();
	}

	public Transaction getTransaction() {
		return transaction;
	}

	public Account getAccount() {
		return account;
	}

	public OperationDirection getDirection() {
		return direction;
	}

	public long getAmount() {
		return amount;
	}

	public long getBalanceAfter() {
		return balanceAfter;
	}
}
