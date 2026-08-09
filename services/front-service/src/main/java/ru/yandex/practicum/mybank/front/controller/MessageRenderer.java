package ru.yandex.practicum.mybank.front.controller;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.mybank.front.client.GatewayException;
import ru.yandex.practicum.mybank.front.client.dto.ErrorResponse;

import java.util.List;
import java.util.Locale;

@Component
public class MessageRenderer {

	private static final String UNKNOWN_ERROR = "error.unknown";

	private static final Logger log = LoggerFactory.getLogger(MessageRenderer.class);

	private final MessageSource messageSource;

	public MessageRenderer(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public List<String> errorMessages(GatewayException exception) {
		ErrorResponse response = exception.getResponse();

		if (response == null) {
			return List.of(unknownErrorMessage());
		}

		if (response.validationErrors() != null) {
			return response.validationErrors().fields().stream()
					.map(field -> errorMessage("error.field." + field.field()))
					.distinct()
					.toList();
		}

		return List.of(errorMessage("error." + response.code()));
	}

	public @Nullable String infoMessage(String key, Object... args) {
		String text = messageSource.getMessage(key, args, null, Locale.of("ru"));

		if (text == null) {
			log.warn("No message for key {}", key);
		}

		return text;
	}

	public String errorMessage(String key) {
		String text = messageSource.getMessage(key, null, null, Locale.of("ru"));

		return text != null ? text : unknownErrorMessage();
	}

	private String unknownErrorMessage() {
		return messageSource.getMessage(UNKNOWN_ERROR, null, Locale.of("ru"));
	}
}
