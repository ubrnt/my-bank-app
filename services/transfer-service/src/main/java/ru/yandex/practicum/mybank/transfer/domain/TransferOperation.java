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

	@Column(name = "transaction_uuid", nullable = false, updatable = false)
	private UUID transactionUuid;

	@Column(name = "from_account_uuid")
	private UUID fromAccountUuid;

	@Column(name = "from_customer_uuid")
	private UUID fromCustomerUuid;

	@Column(name = "to_account_uuid")
	private UUID toAccountUuid;

	@Column(name = "to_customer_uuid")
	private UUID toCustomerUuid;

	@Column(nullable = false, updatable = false)
	private long amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private TransferOperationStatus status;

	@Column(name = "failure_reason", length = 64)
	private String failureReason;

	protected TransferOperation() {
	}

	private TransferOperation(UUID transactionUuid, long amount, TransferOperationStatus status) {
		this.transactionUuid = transactionUuid;
		this.amount = amount;
		this.status = status;
	}

	public static TransferOperation completed(UUID transactionUuid, long amount,
			UUID fromAccountUuid, UUID fromCustomerUuid,
			UUID toAccountUuid, UUID toCustomerUuid) {
		TransferOperation operation = new TransferOperation(transactionUuid, amount, TransferOperationStatus.COMPLETED);
		operation.fromAccountUuid = fromAccountUuid;
		operation.fromCustomerUuid = fromCustomerUuid;
		operation.toAccountUuid = toAccountUuid;
		operation.toCustomerUuid = toCustomerUuid;

		return operation;
	}

	public static TransferOperation failed(UUID transactionUuid, long amount, String failureReason) {
		TransferOperation operation = new TransferOperation(transactionUuid, amount, TransferOperationStatus.FAILED);
		operation.failureReason = failureReason;

		return operation;
	}

	public UUID getTransactionUuid() {
		return transactionUuid;
	}

	public UUID getFromAccountUuid() {
		return fromAccountUuid;
	}

	public UUID getFromCustomerUuid() {
		return fromCustomerUuid;
	}

	public UUID getToAccountUuid() {
		return toAccountUuid;
	}

	public UUID getToCustomerUuid() {
		return toCustomerUuid;
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
