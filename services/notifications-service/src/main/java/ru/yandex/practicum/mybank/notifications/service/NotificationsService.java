package ru.yandex.practicum.mybank.notifications.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mybank.notifications.client.AccountsClient;
import ru.yandex.practicum.mybank.notifications.client.dto.CustomerResponse;
import ru.yandex.practicum.mybank.notifications.domain.EventType;
import ru.yandex.practicum.mybank.notifications.domain.Notification;
import ru.yandex.practicum.mybank.notifications.repository.NotificationRepository;
import tools.jackson.databind.JsonNode;

import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationsService {

	private static final Logger log = LoggerFactory.getLogger(NotificationsService.class);

	private final NotificationRepository notificationRepository;
	private final MessageRenderer messageRenderer;
	private final AccountsClient accountsClient;

	public NotificationsService(NotificationRepository notificationRepository, MessageRenderer messageRenderer,
			AccountsClient accountsClient) {
		this.notificationRepository = notificationRepository;
		this.messageRenderer = messageRenderer;
		this.accountsClient = accountsClient;
	}

	@Transactional
	public void receive(UUID eventUuid, EventType type, UUID recipientUuid, JsonNode payload) {
		CustomerResponse recipient = accountsClient.getCustomer(recipientUuid);
		String message = messageRenderer.render(type, payload);

		Optional<Notification> inserted = notificationRepository.insertIfAbsent(
				UUID.randomUUID(), eventUuid, recipientUuid, type.name(), payload.toString(), message);

		if (inserted.isEmpty()) {
			log.debug("Notification for event {} already accepted", eventUuid);
			return;
		}

		log.info("Notification to {} ({}): {}", recipient.login(), recipient.name(), message);
	}
}
