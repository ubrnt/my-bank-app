package ru.yandex.practicum.mybank.notifications.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mybank.notifications.client.AccountsClient;
import ru.yandex.practicum.mybank.notifications.client.UnknownRecipientException;
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
	private final NotificationsMetrics metrics;

	public NotificationsService(NotificationRepository notificationRepository, MessageRenderer messageRenderer,
			AccountsClient accountsClient, NotificationsMetrics metrics) {
		this.notificationRepository = notificationRepository;
		this.messageRenderer = messageRenderer;
		this.accountsClient = accountsClient;
		this.metrics = metrics;
	}

	public void receive(UUID eventUuid, EventType type, UUID recipientUuid, JsonNode payload) {
		if (notificationRepository.existsByEventUuid(eventUuid)) {
			log.debug("Notification for event {} already sent", eventUuid);
			return;
		}

		CustomerResponse recipient;
		try {
			recipient = accountsClient.getCustomer(recipientUuid);
		} catch (UnknownRecipientException e) {
			metrics.deliveryFailed(NotificationsMetrics.UNKNOWN_LOGIN, "unknown_recipient");

			throw e;
		}

		String message;
		try {
			message = messageRenderer.render(type, payload);
		} catch (InvalidEventException e) {
			metrics.deliveryFailed(recipient.login(), "invalid_event");

			throw e;
		}

		log.info("Notification to {} ({}): {}", recipient.login(), recipient.name(), message);

		notificationRepository.insertIfAbsent(
				UUID.randomUUID(), eventUuid, recipientUuid, type.name(), payload.toString(), message);
	}
}
