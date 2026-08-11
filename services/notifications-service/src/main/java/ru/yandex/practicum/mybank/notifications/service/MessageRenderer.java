package ru.yandex.practicum.mybank.notifications.service;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.mybank.notifications.domain.EventType;
import tools.jackson.databind.JsonNode;

import java.util.Locale;

@Component
public class MessageRenderer {

	private static final int VISIBLE_NUMBER_DIGITS = 4;

	private final MessageSource messageSource;

	public MessageRenderer(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public String render(EventType type, JsonNode payload) {
		Object[] args = switch (type) {
			case MONEY_DEPOSITED -> new Object[]{
					mask(text(payload, "operation", "toNumber")),
					number(payload, "operation", "amount"),
					number(payload, "operation", "balanceAfter")};
			case MONEY_WITHDRAWN -> new Object[]{
					mask(text(payload, "operation", "fromNumber")),
					number(payload, "operation", "amount"),
					number(payload, "operation", "balanceAfter")};
			case MONEY_SENT -> new Object[]{
					mask(text(payload, "operation", "fromNumber")),
					number(payload, "operation", "amount"),
					mask(text(payload, "operation", "toNumber")),
					number(payload, "operation", "balanceAfter")};
			case MONEY_RECEIVED -> new Object[]{
					mask(text(payload, "operation", "toNumber")),
					number(payload, "operation", "amount"),
					mask(text(payload, "operation", "fromNumber")),
					number(payload, "operation", "balanceAfter")};
			case CUSTOMER_UPDATED -> new Object[]{};
		};

		return messageSource.getMessage("notification." + type.name(), args, Locale.of("ru"));
	}

	private String mask(String accountNumber) {
		return "*" + accountNumber.substring(Math.max(0, accountNumber.length() - VISIBLE_NUMBER_DIGITS));
	}

	private long number(JsonNode payload, String... path) {
		return node(payload, path).asLong();
	}

	private String text(JsonNode payload, String... path) {
		return node(payload, path).asString();
	}

	private JsonNode node(JsonNode payload, String... path) {
		JsonNode node = payload;

		for (String field : path) {
			node = node.get(field);

			if (node == null || node.isNull()) {
				throw new InvalidEventException("Event payload lacks field '%s'".formatted(String.join(".", path)));
			}
		}

		return node;
	}
}
