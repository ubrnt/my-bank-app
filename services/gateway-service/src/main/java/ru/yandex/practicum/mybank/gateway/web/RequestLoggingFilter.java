package ru.yandex.practicum.mybank.gateway.web;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RequestLoggingFilter implements WebFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

	private final String actuatorBasePath;

	public RequestLoggingFilter(@Value("${management.endpoints.web.base-path:/actuator}") String actuatorBasePath) {
		this.actuatorBasePath = actuatorBasePath;
	}

	@Override
	public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();

		if (request.getPath().value().startsWith(actuatorBasePath)) {
			return chain.filter(exchange);
		}

		log.info("received {} {}", request.getMethod(), request.getPath().value());

		long startedAt = System.nanoTime();

		return chain.filter(exchange).doFinally(signal -> log.info("handled {} {} {} in {} ms",
				request.getMethod(), request.getPath().value(), exchange.getResponse().getStatusCode(),
				Duration.ofNanos(System.nanoTime() - startedAt).toMillis()));
	}
}
