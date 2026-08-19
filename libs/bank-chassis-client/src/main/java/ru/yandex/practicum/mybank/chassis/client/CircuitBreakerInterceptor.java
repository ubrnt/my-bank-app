package ru.yandex.practicum.mybank.chassis.client;

import org.jspecify.annotations.NonNull;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.UncheckedIOException;

public class CircuitBreakerInterceptor implements ClientHttpRequestInterceptor {

	private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
	private final String circuitBreakerId;
	private final CircuitBreakerPolicy policy;

	public CircuitBreakerInterceptor(CircuitBreakerFactory<?, ?> circuitBreakerFactory, String circuitBreakerId,
			CircuitBreakerPolicy policy) {
		this.circuitBreakerFactory = circuitBreakerFactory;
		this.circuitBreakerId = circuitBreakerId;
		this.policy = policy;
	}

	@Override
	public ClientHttpResponse intercept(@NonNull HttpRequest request, @NonNull byte[] body,
			@NonNull ClientHttpRequestExecution execution) {
		return circuitBreakerFactory.create(circuitBreakerId).run(
				() -> {
					try {
						ClientHttpResponse response = execution.execute(request, body);

						if (policy == CircuitBreakerPolicy.TRANSPORT_AND_SERVER_ERRORS
							&& response.getStatusCode().is5xxServerError()) {
							HttpStatusCode status = response.getStatusCode();
							response.close();

							throw new ServiceCallException(
									"Call to " + circuitBreakerId + " failed with status " + status);
						}

						return response;
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				},
				cause -> {
					throw translate(cause);
				});
	}

	private ServiceCallException translate(Throwable cause) {
		if (cause instanceof ServiceCallException serviceCallException) {
			return serviceCallException;
		}

		if (cause instanceof UncheckedIOException uncheckedIOException) {
			return new ServiceCallException("Call to " + circuitBreakerId + " failed", uncheckedIOException.getCause());
		}

		return new ServiceCallException("Call to " + circuitBreakerId + " failed", cause);
	}
}
