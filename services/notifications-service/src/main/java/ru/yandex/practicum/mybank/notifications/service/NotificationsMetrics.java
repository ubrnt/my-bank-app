package ru.yandex.practicum.mybank.notifications.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class NotificationsMetrics {

	public static final String DELIVERY_FAILURES = "bank.notification.delivery.failures";

	private final MeterRegistry meterRegistry;

	public NotificationsMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void deliveryFailed(String reason) {
		Counter.builder(DELIVERY_FAILURES)
				.description("Notifications that could not be delivered")
				.tag("reason", reason)
				.register(meterRegistry)
				.increment();
	}
}
