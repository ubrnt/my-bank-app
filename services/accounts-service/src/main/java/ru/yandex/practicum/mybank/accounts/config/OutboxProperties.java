package ru.yandex.practicum.mybank.accounts.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("mybank.outbox")
public record OutboxProperties(
		Duration pollInterval,
		int batchSize,
		Duration staleTimeout,
		int maxAttempts
) {
}
