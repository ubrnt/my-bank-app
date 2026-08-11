package ru.yandex.practicum.mybank.notifications.service;

public class InvalidEventException extends RuntimeException {

	public InvalidEventException(String message) {
		super(message);
	}
}
