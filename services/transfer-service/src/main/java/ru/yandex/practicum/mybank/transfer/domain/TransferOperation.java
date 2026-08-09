package ru.yandex.practicum.mybank.transfer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import ru.yandex.practicum.mybank.persistence.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "transfer_operations")
public class TransferOperation extends BaseEntity {

	@Column(name = "from_customer_login", nullable = false, updatable = false)
	private String fromCustomerLogin;

	@Column(name = "to_customer_login", nullable = false, updatable = false)
	private String toCustomerLogin;

	@Column(name = "from_customer_uuid")
	private UUID fromCustomerUuid;

	@Column(name = "from_account_uuid")
	private UUID fromAccountUuid;

	@Column(name = "to_customer_uuid")
	private UUID toCustomerUuid;

	@Column(name = "to_account_uuid")
	private UUID toAccountUuid;

	@Column(nullable = false, updatable = false)
	private long amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private TransferOperationStatus status;

	@Column(name = "failure_reason", length = 64)
	private String failureReason;

	protected TransferOperation() {
	}

	public TransferOperation(UUID uuid, String fromCustomerLogin, long amount) {
		super(uuid);
		this.fromCustomerLogin = fromCustomerLogin;
		this.amount = amount;
		this.status = TransferOperationStatus.PENDING;
	}

	public boolean matches(String fromCustomerLogin, String toCustomerLogin, long amount) {
		return this.fromCustomerLogin.equals(fromCustomerLogin)
			&& this.toCustomerLogin.equals(toCustomerLogin)
			&& this.amount == amount;
	}

	public void complete(UUID fromAccountUuid, UUID fromCustomerUuid, UUID toAccountUuid, UUID toCustomerUuid) {
		this.fromAccountUuid = fromAccountUuid;
		this.fromCustomerUuid = fromCustomerUuid;
		this.toAccountUuid = toAccountUuid;
		this.toCustomerUuid = toCustomerUuid;
		this.status = TransferOperationStatus.COMPLETED;
		this.failureReason = null;
	}

	public void fail(String failureReason) {
		this.status = TransferOperationStatus.FAILED;
		this.failureReason = failureReason;
	}

	public String getToCustomerLogin() {
		return toCustomerLogin;
	}

	public String getFromCustomerLogin() {
		return fromCustomerLogin;
	}

	public UUID getFromCustomerUuid() {
		return fromCustomerUuid;
	}

	public UUID getFromAccountUuid() {
		return fromAccountUuid;
	}

	public UUID getToCustomerUuid() {
		return toCustomerUuid;
	}

	public UUID getToAccountUuid() {
		return toAccountUuid;
	}

	public long getAmount() {
		return amount;
	}

	public TransferOperationStatus getStatus() {
		return status;
	}

	public String getFailureReason() {
		return failureReason;
	}
}
