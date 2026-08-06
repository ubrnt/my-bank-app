package ru.yandex.practicum.mybank.notifications.outbox;

public enum NotificationsOutboxStatus {
	PENDING,
	PROCESSING,
	PROCESSED,
	FAILED
}
