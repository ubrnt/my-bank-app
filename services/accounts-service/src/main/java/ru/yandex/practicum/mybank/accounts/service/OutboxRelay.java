package ru.yandex.practicum.mybank.accounts.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.mybank.accounts.client.NotificationsClient;
import ru.yandex.practicum.mybank.accounts.client.dto.NotificationRequest;
import ru.yandex.practicum.mybank.accounts.domain.OutboxEvent;

import java.util.List;

@Component
public class OutboxRelay {

	private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

	private final OutboxService outboxService;
	private final NotificationsClient notificationsClient;

	public OutboxRelay(OutboxService outboxService, NotificationsClient notificationsClient) {
		this.outboxService = outboxService;
		this.notificationsClient = notificationsClient;
	}

	@Scheduled(fixedDelayString = "${mybank.outbox.poll-interval}")
	public void relayPending() {
		List<OutboxEvent> claimed = outboxService.claim();
		if (claimed.isEmpty()) {
			return;
		}

		log.debug("Claimed {} outbox events", claimed.size());
		claimed.forEach(this::send);
	}

	private void send(OutboxEvent event) {
		try {
			notificationsClient.send(new NotificationRequest(
					event.getUuid(), event.getEventType(), event.getRecipient(), event.getPayload()));

			outboxService.markProcessed(event.getId());
		} catch (RuntimeException e) {
			outboxService.markNotDelivered(event.getId(), NestedExceptionUtils.getMostSpecificCause(e).toString());

			log.warn("Outbox event {} not delivered on attempt {}",
					event.getUuid(), event.getAttempts() + 1, e);
		}
	}
}
