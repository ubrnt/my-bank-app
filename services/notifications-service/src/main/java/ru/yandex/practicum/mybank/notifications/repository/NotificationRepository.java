package ru.yandex.practicum.mybank.notifications.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.mybank.notifications.domain.Notification;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	boolean existsByEventUuid(UUID eventUuid);

	@Query(value = """
			insert into notifications (uuid, event_uuid, customer_uuid, type, payload, message,
			                           created_ts, updated_ts, version)
			values (:uuid, :eventUuid, :customerUuid, :type, cast(:payload as jsonb), :message, now(), now(), 0)
			on conflict do nothing
			returning *
			""", nativeQuery = true)
	Optional<Notification> insertIfAbsent(UUID uuid, UUID eventUuid, UUID customerUuid, String type,
			String payload, String message);
}
