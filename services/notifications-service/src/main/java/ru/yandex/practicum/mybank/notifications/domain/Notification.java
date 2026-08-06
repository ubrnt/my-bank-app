package ru.yandex.practicum.mybank.notifications.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import ru.yandex.practicum.mybank.persistence.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

	@Column(name = "event_uuid", nullable = false, updatable = false)
	private UUID eventUuid;

	@Column(name = "customer_uuid", nullable = false, updatable = false)
	private UUID customerUuid;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 32)
	private EventType type;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, updatable = false)
	private String payload;

	@Column(nullable = false, updatable = false)
	private String message;

	protected Notification() {
	}

	public Notification(UUID eventUuid, UUID customerUuid, EventType type, String payload, String message) {
		this.eventUuid = eventUuid;
		this.customerUuid = customerUuid;
		this.type = type;
		this.payload = payload;
		this.message = message;
	}

	public UUID getEventUuid() {
		return eventUuid;
	}

	public UUID getCustomerUuid() {
		return customerUuid;
	}

	public EventType getType() {
		return type;
	}

	public String getPayload() {
		return payload;
	}

	public String getMessage() {
		return message;
	}
}
