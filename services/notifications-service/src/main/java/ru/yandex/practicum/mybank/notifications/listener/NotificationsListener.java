package ru.yandex.practicum.mybank.notifications.listener;

import jakarta.validation.Valid;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.mybank.notifications.dto.NotificationEvent;
import ru.yandex.practicum.mybank.notifications.service.NotificationsService;

@Component
public class NotificationsListener {

	private final NotificationsService notificationsService;

	public NotificationsListener(NotificationsService notificationsService) {
		this.notificationsService = notificationsService;
	}

	@KafkaListener(topics = "${mybank.notifications.topic}")
	public void receive(@Valid NotificationEvent event) {
		notificationsService.receive(event.eventUuid(), event.type(), event.recipientUuid(), event.payload());
	}
}
