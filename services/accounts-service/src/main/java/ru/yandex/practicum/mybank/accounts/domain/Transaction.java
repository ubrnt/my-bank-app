package ru.yandex.practicum.mybank.accounts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import ru.yandex.practicum.mybank.persistence.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 16)
	private TransactionType type;

	protected Transaction() {
	}

	public Transaction(UUID uuid, TransactionType type) {
		super(uuid);
		this.type = type;
	}

	public TransactionType getType() {
		return type;
	}
}
