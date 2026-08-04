package ru.yandex.practicum.mybank.accounts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, updatable = false, length = 32)
	private EventType eventType;

	@Enumerated(EnumType.STRING)
	@Column(name = "aggregate_type", nullable = false, updatable = false, length = 32)
	private AggregateType aggregateType;

	@Column(name = "aggregate_id", nullable = false, updatable = false)
	private long aggregateId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, updatable = false)
	private String payload;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private OutboxStatus status;

	@Column(name = "locked_at")
	private Instant lockedAt;

	@Column(name = "processed_at")
	private Instant processedAt;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "last_error")
	private String lastError;

	protected OutboxEvent() {
	}

	public OutboxEvent(EventType eventType, AggregateType aggregateType, long aggregateId, String payload) {
		this.eventType = eventType;
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.payload = payload;
		this.status = OutboxStatus.PENDING;
	}

	public EventType getEventType() {
		return eventType;
	}

	public AggregateType getAggregateType() {
		return aggregateType;
	}

	public long getAggregateId() {
		return aggregateId;
	}

	public String getPayload() {
		return payload;
	}

	public OutboxStatus getStatus() {
		return status;
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
