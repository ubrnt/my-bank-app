package ru.yandex.practicum.mybank.accounts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "balance_operations")
public class BalanceOperation extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false, updatable = false)
	private Account account;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 16)
	private OperationType type;

	@Column(nullable = false, updatable = false)
	private long amount;

	protected BalanceOperation() {
	}

	public BalanceOperation(UUID uuid, Account account, OperationType type, long amount) {
		setUuid(uuid);
		this.account = account;
		this.type = type;
		this.amount = amount;
	}

	public Account getAccount() {
		return account;
	}

	public OperationType getType() {
		return type;
	}

	public long getAmount() {
		return amount;
	}
}
