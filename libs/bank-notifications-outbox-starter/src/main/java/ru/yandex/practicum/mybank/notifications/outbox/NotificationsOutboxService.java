package ru.yandex.practicum.mybank.notifications.outbox;

import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class NotificationsOutboxService {

	private final NotificationsOutboxEventRepository notificationsOutboxEventRepository;
	private final ObjectMapper objectMapper;
	private final NotificationsOutboxProperties properties;

	public NotificationsOutboxService(NotificationsOutboxEventRepository notificationsOutboxEventRepository, ObjectMapper objectMapper,
			NotificationsOutboxProperties properties) {
		this.notificationsOutboxEventRepository = notificationsOutboxEventRepository;
		this.objectMapper = objectMapper;
		this.properties = properties;
	}

	@Transactional
	public void save(String eventType, String aggregateType, long aggregateId, UUID recipientUuid,
			Object payload) {
		String payloadJson = objectMapper.writeValueAsString(payload);

		notificationsOutboxEventRepository.save(
				new NotificationsOutboxEvent(eventType, aggregateType, aggregateId, recipientUuid, payloadJson));
	}

	@Transactional
	public List<NotificationsOutboxEvent> claim() {
		return notificationsOutboxEventRepository.claim(properties.staleTimeout().toSeconds(), properties.batchSize());
	}

	@Transactional
	public void markProcessed(long id) {
		notificationsOutboxEventRepository.findById(id).orElseThrow().markProcessed();
	}

	@Transactional
	public void markNotDelivered(long id, String error) {
		NotificationsOutboxEvent event = notificationsOutboxEventRepository.findById(id).orElseThrow();

		if (event.getAttempts() + 1 >= properties.maxAttempts()) {
			event.markFailed(error);
		} else {
			Duration retryDelay = calculateRetryDelay(event.getAttempts());

			event.markPending(error, notificationsOutboxEventRepository.currentTimestamp().plus(retryDelay));
		}
	}

	private Duration calculateRetryDelay(int attempts) {
		Duration maxRetryDelay = properties.maxRetryDelay();

		long multiplier = (long) Math.pow(2, attempts);
		Duration retryDelay = properties.retryDelay().multipliedBy(multiplier);

		return retryDelay.compareTo(maxRetryDelay) > 0 ? maxRetryDelay : retryDelay;
	}
}
