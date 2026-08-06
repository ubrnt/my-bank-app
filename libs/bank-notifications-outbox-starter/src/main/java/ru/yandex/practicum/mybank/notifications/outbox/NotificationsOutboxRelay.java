package ru.yandex.practicum.mybank.notifications.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.scheduling.annotation.Scheduled;
import ru.yandex.practicum.mybank.chassis.worker.BatchProcessor;

public class NotificationsOutboxRelay {

	private static final Logger log = LoggerFactory.getLogger(NotificationsOutboxRelay.class);

	private final BatchProcessor<NotificationsOutboxEvent> processor;

	public NotificationsOutboxRelay(NotificationsOutboxService notificationsOutboxService, NotificationsClient notificationsClient) {
		this.processor = new BatchProcessor<>(
				notificationsOutboxService::claim,
				event -> notificationsClient.send(new NotificationRequest(
						event.getUuid(), event.getEventType(), event.getRecipientUuid(), event.getPayload())),
				event -> notificationsOutboxService.markProcessed(event.getId()),
				(event, e) -> {
					notificationsOutboxService.markNotDelivered(event.getId(),
							NestedExceptionUtils.getMostSpecificCause(e).toString());

					log.warn("Outbox event {} not delivered on attempt {}",
							event.getUuid(), event.getAttempts() + 1, e);
				});
	}

	@Scheduled(fixedDelayString = "${mybank.notifications.outbox.poll-interval}")
	public void relayPending() {
		processor.run();
	}
}
