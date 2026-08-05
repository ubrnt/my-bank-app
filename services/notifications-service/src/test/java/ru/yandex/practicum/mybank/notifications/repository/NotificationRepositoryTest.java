package ru.yandex.practicum.mybank.notifications.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.mybank.notifications.PostgresContainerConfig;
import ru.yandex.practicum.mybank.notifications.config.JpaConfig;
import ru.yandex.practicum.mybank.notifications.domain.EventType;
import ru.yandex.practicum.mybank.notifications.domain.Notification;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresContainerConfig.class, JpaConfig.class})
class NotificationRepositoryTest {

	private static final UUID EVENT_UUID = UUID.fromString("1b7f4a90-0d51-4c2e-9f77-0a1e5c3b0001");
	private static final UUID CUSTOMER_UUID = UUID.fromString("3f2a77c4-1e08-4a6b-8f21-9c0d5b7e1111");
	private static final String PAYLOAD = "{\"uuid\":\"cccc0001-2222-4333-8444-555566660001\",\"type\":\"DEPOSIT\"}";
	private static final String MESSAGE = "Счёт 40817810000000000001: пополнение на 5000";

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private EntityManager entityManager;

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@Test
	void insertsAndReadsNotification() {
		Optional<Notification> inserted = insert(UUID.randomUUID());

		assertThat(inserted).isPresent();
		entityManager.clear();

		Notification reloaded = notificationRepository.findById(inserted.orElseThrow().getId()).orElseThrow();
		assertThat(reloaded.getEventUuid()).isEqualTo(EVENT_UUID);
		assertThat(reloaded.getCustomerUuid()).isEqualTo(CUSTOMER_UUID);
		assertThat(reloaded.getType()).isEqualTo(EventType.MONEY_DEPOSITED);
		assertThat(jsonMapper.readTree(reloaded.getPayload())).isEqualTo(jsonMapper.readTree(PAYLOAD));
		assertThat(reloaded.getMessage()).isEqualTo(MESSAGE);
		assertThat(reloaded.getCreatedTs()).isNotNull();
	}

	@Test
	void ignoresSecondNotificationForSameEvent() {
		insert(UUID.randomUUID());

		Optional<Notification> duplicate = insert(UUID.randomUUID());

		assertThat(duplicate).isEmpty();
		assertThat(notificationRepository.count()).isEqualTo(1);
	}

	private Optional<Notification> insert(UUID uuid) {
		return notificationRepository.insertIfAbsent(
				uuid, EVENT_UUID, CUSTOMER_UUID, EventType.MONEY_DEPOSITED.name(), PAYLOAD, MESSAGE);
	}
}
