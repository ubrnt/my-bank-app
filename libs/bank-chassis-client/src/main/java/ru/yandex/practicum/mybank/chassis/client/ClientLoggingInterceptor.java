package ru.yandex.practicum.mybank.chassis.client;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.time.Duration;

public class ClientLoggingInterceptor implements ClientHttpRequestInterceptor {

	private static final Logger log = LoggerFactory.getLogger(ClientLoggingInterceptor.class);

	private final String serviceId;

	public ClientLoggingInterceptor(String serviceId) {
		this.serviceId = serviceId;
	}

	@Override
	public @NonNull ClientHttpResponse intercept(@NonNull HttpRequest request, @NonNull byte[] body,
			@NonNull ClientHttpRequestExecution execution) throws IOException {
		String path = request.getURI().getPath();

		log.debug("calling {} {} {}", serviceId, request.getMethod(), path);

		long startedAt = System.nanoTime();

		try {
			ClientHttpResponse response = execution.execute(request, body);

			log.debug("finished {} {} {} {} in {} ms", serviceId, request.getMethod(), path, response.getStatusCode(),
					elapsedMillis(startedAt));

			return response;
		} catch (Exception e) {
			log.warn("failed {} {} {} in {} ms: {}", serviceId, request.getMethod(), path, elapsedMillis(startedAt),
					e.toString());

			throw e;
		}
	}

	private long elapsedMillis(long startedAt) {
		return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
	}
}
