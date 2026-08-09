package ru.yandex.practicum.mybank.chassis.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

public class RequestLoggingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

	private final String skippedPrefix;

	public RequestLoggingFilter(String skippedPrefix) {
		this.skippedPrefix = skippedPrefix;
	}

	@Override
	protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
		return request.getRequestURI().startsWith(skippedPrefix);
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain chain) throws ServletException, IOException {
		log.info("received {} {}", request.getMethod(), request.getRequestURI());

		long startedAt = System.nanoTime();

		try {
			chain.doFilter(request, response);

			log.info("handled {} {} {} in {} ms", request.getMethod(), request.getRequestURI(), response.getStatus(),
					elapsedMillis(startedAt));
		} catch (Exception e) {
			log.warn("failed {} {} in {} ms: {}", request.getMethod(), request.getRequestURI(),
					elapsedMillis(startedAt), e.toString());

			throw e;
		}
	}

	private long elapsedMillis(long startedAt) {
		return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
	}
}
