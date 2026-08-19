package ru.yandex.practicum.mybank.notifications.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class NotificationsMetrics {

	public static final String DELIVERY_FAILURES = "bank.notification.delivery.failures";
	public static final String UNKNOWN_LOGIN = "unknown";

	private final MeterRegistry meterRegistry;

	public NotificationsMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void deliveryFailed(String login, String reason) {
		Counter.builder(DELIVERY_FAILURES)
				.description("Notifications that could not be delivered")
				.tag("login", login)
				.tag("reason", reason)
				.register(meterRegistry)
				.increment();
	}
}
