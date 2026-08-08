package ru.yandex.practicum.mybank.cash.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import ru.yandex.practicum.mybank.persistence.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "cash_operations")
public class CashOperation extends BaseEntity {

	@Column(name = "customer_login", nullable = false, updatable = false)
	private String customerLogin;

	@Column(name = "customer_uuid")
	private UUID customerUuid;

	@Column(name = "account_uuid")
	private UUID accountUuid;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 16)
	private CashOperationType type;

	@Column(nullable = false, updatable = false)
	private long amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private CashOperationStatus status;

	@Column(name = "failure_reason", length = 64)
	private String failureReason;

	protected CashOperation() {
	}

	public CashOperation(UUID uuid, String customerLogin, CashOperationType type, long amount) {
		super(uuid);
		this.customerLogin = customerLogin;
		this.type = type;
		this.amount = amount;
		this.status = CashOperationStatus.PENDING;
	}

	public void complete(UUID accountUuid, UUID customerUuid) {
		this.accountUuid = accountUuid;
		this.customerUuid = customerUuid;
		this.status = CashOperationStatus.COMPLETED;
		this.failureReason = null;
	}

	public void fail(String failureReason) {
		this.status = CashOperationStatus.FAILED;
		this.failureReason = failureReason;
	}

	public String getCustomerLogin() {
		return customerLogin;
	}

	public UUID getAccountUuid() {
		return accountUuid;
	}

	public UUID getCustomerUuid() {
		return customerUuid;
	}

	public CashOperationType getType() {
		return type;
	}

	public long getAmount() {
		return amount;
	}

	public CashOperationStatus getStatus() {
		return status;
	}

	public String getFailureReason() {
		return failureReason;
	}
}
