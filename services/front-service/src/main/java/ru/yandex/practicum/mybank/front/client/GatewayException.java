package ru.yandex.practicum.mybank.front.client;

import org.jspecify.annotations.Nullable;
import ru.yandex.practicum.mybank.front.client.dto.ErrorResponse;

public class GatewayException extends RuntimeException {

	private final transient @Nullable ErrorResponse response;

	public GatewayException(@Nullable ErrorResponse response, Throwable cause) {
		super(cause);
		this.response = response;
	}

	public @Nullable ErrorResponse getResponse() {
		return response;
	}
}
