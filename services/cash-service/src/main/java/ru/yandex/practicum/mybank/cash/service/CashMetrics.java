package ru.yandex.practicum.mybank.cash.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.mybank.cash.domain.CashOperationType;

import java.util.Locale;

@Component
public class CashMetrics {

	public static final String OPERATION_FAILURES = "bank.cash.operation.failures";

	private final MeterRegistry meterRegistry;

	public CashMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void operationFailed(CashOperationType type, String reason) {
		Counter.builder(OPERATION_FAILURES)
				.description("Failed cash operation attempts")
				.tag("type", type.name().toLowerCase(Locale.ROOT))
				.tag("reason", reason)
				.register(meterRegistry)
				.increment();
	}
}
