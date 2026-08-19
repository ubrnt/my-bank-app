package ru.yandex.practicum.mybank.notifications.outbox;

import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class NotificationsEventPublisher {

	private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
	private final String topic;
	private final Duration sendTimeout;

	public NotificationsEventPublisher(KafkaTemplate<String, NotificationEvent> kafkaTemplate, String topic,
			Duration sendTimeout) {
		this.kafkaTemplate = kafkaTemplate;
		this.topic = topic;
		this.sendTimeout = sendTimeout;
	}

	public void send(NotificationEvent event) {
		String key = event.recipientUuid().toString();

		try {
			kafkaTemplate.send(topic, key, event)
					.orTimeout(sendTimeout.toMillis(), TimeUnit.MILLISECONDS)
					.join();
		} catch (Exception e) {
			throw new NotificationDeliveryException(event.eventUuid(), e);
		}
	}
}
