package ru.yandex.practicum.mybank.chassis.web;

public class MissingUsernameClaimException extends RuntimeException {

	public MissingUsernameClaimException() {
		super("Access token carries no preferred_username claim");
	}
}
