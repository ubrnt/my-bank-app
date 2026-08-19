package ru.yandex.practicum.mybank.notifications.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("mybank.notifications.outbox")
public record NotificationsOutboxProperties(
		@DefaultValue("5s") Duration pollInterval,
		@DefaultValue("20") int batchSize,
		@DefaultValue("5m") Duration staleTimeout,
		@DefaultValue("6") int maxAttempts,
		@DefaultValue("5s") Duration retryDelay,
		@DefaultValue("1m") Duration maxRetryDelay,
		@DefaultValue("10s") Duration sendTimeout
) {
}
