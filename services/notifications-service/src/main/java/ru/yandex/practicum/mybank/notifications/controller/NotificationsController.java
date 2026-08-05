package ru.yandex.practicum.mybank.notifications.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.mybank.notifications.dto.NotificationRequest;
import ru.yandex.practicum.mybank.notifications.service.NotificationsService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationsController {

	private final NotificationsService notificationsService;

	public NotificationsController(NotificationsService notificationsService) {
		this.notificationsService = notificationsService;
	}

	@PostMapping
	public void receive(@Valid @RequestBody NotificationRequest request) {
		notificationsService.receive(request.eventUuid(), request.type(), request.recipientUuid(), request.payload());
	}
}
