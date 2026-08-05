package ru.yandex.practicum.mybank.notifications.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, updatable = false)
	private UUID uuid = UUID.randomUUID();

	@CreatedDate
	@Column(name = "created_ts", nullable = false, updatable = false)
	private Instant createdTs;

	@LastModifiedDate
	@Column(name = "updated_ts", nullable = false)
	private Instant updatedTs;

	@Version
	private Long version;

	protected BaseEntity() {
	}

	public Long getId() {
		return id;
	}

	public UUID getUuid() {
		return uuid;
	}

	public Instant getCreatedTs() {
		return createdTs;
	}

	public Instant getUpdatedTs() {
		return updatedTs;
	}

	public Long getVersion() {
		return version;
	}
}
