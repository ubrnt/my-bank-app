package ru.yandex.practicum.mybank.notifications.service;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import ru.yandex.practicum.mybank.notifications.domain.EventType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageRendererTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final MessageRenderer messageRenderer = new MessageRenderer(messageSource());

	@Test
	void rendersDeposit() {
		JsonNode payload = operation("""
				{"direction": "DEPOSIT", "toLogin": "user1", "toNumber": "40817810000000000001",
				 "amount": 5000, "balanceAfter": 105000}
				""");

		assertThat(messageRenderer.render(EventType.MONEY_DEPOSITED, payload))
				.isEqualTo("Счёт *0001: пополнение на 5000. Доступно 105000");
	}

	@Test
	void rendersWithdrawal() {
		JsonNode payload = operation("""
				{"direction": "WITHDRAW", "fromLogin": "user1", "fromNumber": "40817810000000000001",
				 "amount": 500, "balanceAfter": 99500}
				""");

		assertThat(messageRenderer.render(EventType.MONEY_WITHDRAWN, payload))
				.isEqualTo("Счёт *0001: снятие 500. Доступно 99500");
	}

	@Test
	void rendersTransferForSender() {
		assertThat(messageRenderer.render(EventType.MONEY_SENT, transfer(98500)))
				.isEqualTo("Счёт *0001: перевод 3000 на счёт *0002. Доступно 98500");
	}

	@Test
	void rendersTransferForReceiver() {
		assertThat(messageRenderer.render(EventType.MONEY_RECEIVED, transfer(53000)))
				.isEqualTo("Счёт *0002: поступление 3000 со счёта *0001. Доступно 53000");
	}

	@Test
	void rendersProfileUpdate() {
		JsonNode payload = jsonMapper.readTree("""
				{"uuid": "3f2a77c4-1e08-4a6b-8f21-9c0d5b7e1111", "login": "user1",
				 "name": "user1_first_name user1_last_name"}
				""");

		assertThat(messageRenderer.render(EventType.CUSTOMER_UPDATED, payload))
				.isEqualTo("Данные профиля обновлены. Если это были не вы, обратитесь в банк");
	}

	@Test
	void rejectsPayloadWithoutRequiredField() {
		JsonNode payload = jsonMapper.readTree("""
				{"operation": {"amount": 500}}
				""");

		assertThatThrownBy(() -> messageRenderer.render(EventType.MONEY_WITHDRAWN, payload))
				.isInstanceOf(InvalidEventException.class)
				.hasMessageContaining("operation.fromNumber");
	}

	private JsonNode transfer(long balanceAfter) {
		return operation("""
				{"direction": "TRANSFER", "fromLogin": "user1", "fromNumber": "40817810000000000001",
				 "toLogin": "user2", "toNumber": "40817810000000000002",
				 "amount": 3000, "balanceAfter": %d}
				""".formatted(balanceAfter));
	}

	private JsonNode operation(String operationJson) {
		return jsonMapper.readTree("""
				{"uuid": "cccc0001-2222-4333-8444-555566660001", "type": "TRANSFER", "operation": %s}
				""".formatted(operationJson));
	}

	private ResourceBundleMessageSource messageSource() {
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename("messages");
		messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());

		return messageSource;
	}
}
