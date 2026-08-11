package ru.yandex.practicum.mybank.notifications.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import ru.yandex.practicum.mybank.persistence.BaseEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications_outbox")
public class NotificationsOutboxEvent extends BaseEntity {

	@Column(name = "event_type", nullable = false, updatable = false, length = 32)
	private String eventType;

	@Column(name = "aggregate_type", nullable = false, updatable = false, length = 32)
	private String aggregateType;

	@Column(name = "aggregate_id", nullable = false, updatable = false)
	private long aggregateId;

	@Column(name = "recipient_uuid", nullable = false, updatable = false)
	private UUID recipientUuid;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, updatable = false)
	private String payload;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private NotificationsOutboxStatus status;

	@Column(name = "next_attempt_at", nullable = false, insertable = false)
	private Instant nextAttemptAt;

	@Column(name = "locked_at")
	private Instant lockedAt;

	@Column(name = "processed_at")
	private Instant processedAt;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "last_error")
	private String lastError;

	protected NotificationsOutboxEvent() {
	}

	public NotificationsOutboxEvent(String eventType, String aggregateType, long aggregateId, UUID recipientUuid,
			String payload) {
		this.eventType = eventType;
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.recipientUuid = recipientUuid;
		this.payload = payload;
		this.status = NotificationsOutboxStatus.PENDING;
	}

	public void markProcessed() {
		this.status = NotificationsOutboxStatus.PROCESSED;
		this.processedAt = Instant.now();
		this.lockedAt = null;
	}

	public void markPending(String error, Instant nextAttemptAt) {
		this.status = NotificationsOutboxStatus.PENDING;
		this.attempts++;
		this.lastError = error;
		this.lockedAt = null;
		this.nextAttemptAt = nextAttemptAt;
	}

	public void markFailed(String error) {
		this.status = NotificationsOutboxStatus.FAILED;
		this.attempts++;
		this.lastError = error;
		this.lockedAt = null;
	}

	public String getEventType() {
		return eventType;
	}

	public String getAggregateType() {
		return aggregateType;
	}

	public long getAggregateId() {
		return aggregateId;
	}

	public UUID getRecipientUuid() {
		return recipientUuid;
	}

	public String getPayload() {
		return payload;
	}

	public NotificationsOutboxStatus getStatus() {
		return status;
	}

	public Instant getNextAttemptAt() {
		return nextAttemptAt;
	}

	public Instant getLockedAt() {
		return lockedAt;
	}

	public Instant getProcessedAt() {
		return processedAt;
	}

	public int getAttempts() {
		return attempts;
	}

	public String getLastError() {
		return lastError;
	}
}
