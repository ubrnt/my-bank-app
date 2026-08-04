package ru.yandex.practicum.mybank.accounts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import ru.yandex.practicum.mybank.accounts.client.NotificationDeliveryException;
import ru.yandex.practicum.mybank.accounts.client.dto.NotificationRequest;
import ru.yandex.practicum.mybank.accounts.config.OutboxProperties;
import ru.yandex.practicum.mybank.accounts.domain.EventType;
import ru.yandex.practicum.mybank.accounts.service.OutboxRelay;

import java.net.ConnectException;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OutboxRelayIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private OutboxRelay outboxRelay;

	@Autowired
	private OutboxProperties outboxProperties;

	@BeforeEach
	void depositMoney() throws Exception {
		mockMvc.perform(post("/api/transactions/deposit")
						.with(serviceToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"transactionUuid": "cccc0001-2222-4333-8444-555566660001", "login": "user1", "amount": 5000}
								"""))
				.andExpect(status().isOk());
	}

	@Test
	void sendsClaimedEventAndMarksItProcessed() {
		UUID eventUuid = (UUID) event().get("uuid");

		outboxRelay.relayPending();

		ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
		verify(notificationsClient).send(captor.capture());

		NotificationRequest sent = captor.getValue();
		assertThat(sent.eventUuid()).isEqualTo(eventUuid);
		assertThat(sent.type()).isEqualTo(EventType.MONEY_DEPOSITED);
		assertThat(sent.recipient()).contains("\"login\"");
		assertThat(sent.payload()).contains("\"balanceAfter\"");

		Map<String, Object> processed = event();
		assertThat(processed).containsEntry("status", "PROCESSED").containsEntry("attempts", 0);
		assertThat(processed.get("processed_at")).isNotNull();
		assertThat(processed.get("locked_at")).isNull();
	}

	@Test
	void returnsEventToPendingWhenDeliveryFails() {
		doThrow(deliveryFailure()).when(notificationsClient).send(any());

		outboxRelay.relayPending();

		Map<String, Object> pending = event();
		assertThat(pending).containsEntry("status", "PENDING").containsEntry("attempts", 1);
		assertThat(pending.get("last_error")).isEqualTo("java.net.ConnectException: Connection refused");
		assertThat(pending.get("locked_at")).isNull();
	}

	@Test
	void failsEventOnceAttemptsAreSpent() {
		doThrow(deliveryFailure()).when(notificationsClient).send(any());

		for (int attempt = 0; attempt < outboxProperties.maxAttempts(); attempt++) {
			outboxRelay.relayPending();
		}

		assertThat(event())
				.containsEntry("status", "FAILED")
				.containsEntry("attempts", outboxProperties.maxAttempts());
	}

	private NotificationDeliveryException deliveryFailure() {
		return new NotificationDeliveryException(UUID.randomUUID(), new ConnectException("Connection refused"));
	}

	private Map<String, Object> event() {
		return jdbcTemplate.queryForMap("""
				select uuid, status, attempts, last_error, locked_at, processed_at from outbox_events
				""");
	}
}
