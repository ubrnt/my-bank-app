package ru.yandex.practicum.mybank.transfer.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class TransferMetrics {

	public static final String TRANSFER_FAILURES = "bank.transfer.failures";

	private final MeterRegistry meterRegistry;

	public TransferMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void transferFailed(String reason) {
		Counter.builder(TRANSFER_FAILURES)
				.description("Failed money transfer attempts")
				.tag("reason", reason)
				.register(meterRegistry)
				.increment();
	}
}
