package ru.yandex.practicum.mybank.notifications.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum EventType {
	MONEY_DEPOSITED,
	MONEY_WITHDRAWN,
	MONEY_SENT,
	MONEY_RECEIVED,
	PROFILE_UPDATED;

	@JsonCreator
	public static EventType of(String value) {
		return valueOf(value.toUpperCase(Locale.ROOT));
	}

	@JsonValue
	public String value() {
		return name().toLowerCase(Locale.ROOT);
	}
}
