package ru.yandex.practicum.mybank.chassis.client;

import org.springframework.web.client.RestClientException;

public class ServiceCallException extends RestClientException {

	public ServiceCallException(String message) {
		super(message);
	}

	public ServiceCallException(String message, Throwable cause) {
		super(message, cause);
	}
}
