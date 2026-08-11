package ru.yandex.practicum.mybank.accounts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import ru.yandex.practicum.mybank.persistence.BaseEntity;

@Entity
@Table(name = "accounts")
public class Account extends BaseEntity {

	@Column(nullable = false, updatable = false)
	private String number;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false, updatable = false)
	private Customer customer;

	@Column(nullable = false)
	private long balance;

	protected Account() {
	}

	public Account(String number, Customer customer) {
		this.number = number;
		this.customer = customer;
	}

	public String getNumber() {
		return number;
	}

	public Customer getCustomer() {
		return customer;
	}

	public long getBalance() {
		return balance;
	}

	public void deposit(long amount) {
		balance += amount;
	}

	public void withdraw(long amount) {
		balance -= amount;
	}
}
