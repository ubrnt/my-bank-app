package ru.yandex.practicum.mybank.notifications.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("mybank.notifications.outbox")
public record NotificationsOutboxProperties(
		Duration pollInterval,
		int batchSize,
		Duration staleTimeout,
		int maxAttempts,
		@DefaultValue("5s") Duration retryDelay,
		@DefaultValue("1m") Duration maxRetryDelay
) {
}
