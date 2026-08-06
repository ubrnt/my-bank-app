package ru.yandex.practicum.mybank.notifications.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("mybank.notifications.outbox")
public record NotificationsOutboxProperties(
		Duration pollInterval,
		int batchSize,
		Duration staleTimeout,
		int maxAttempts
) {
}
