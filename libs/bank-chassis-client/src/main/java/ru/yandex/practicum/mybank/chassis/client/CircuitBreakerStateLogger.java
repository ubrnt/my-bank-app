package ru.yandex.practicum.mybank.chassis.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CircuitBreakerStateLogger {

	private static final Logger log = LoggerFactory.getLogger(CircuitBreakerStateLogger.class);

	public CircuitBreakerStateLogger(CircuitBreakerRegistry registry) {
		registry.getAllCircuitBreakers().forEach(this::logStateTransitions);
		registry.getEventPublisher().onEntryAdded(event -> logStateTransitions(event.getAddedEntry()));
	}

	private void logStateTransitions(CircuitBreaker circuitBreaker) {
		circuitBreaker.getEventPublisher().onStateTransition(event -> log.info("circuit breaker {}: {} -> {}",
				event.getCircuitBreakerName(),
				event.getStateTransition().getFromState(),
				event.getStateTransition().getToState()));
	}
}
