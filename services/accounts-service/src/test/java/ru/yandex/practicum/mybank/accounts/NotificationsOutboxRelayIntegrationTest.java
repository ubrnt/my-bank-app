package ru.yandex.practicum.mybank.accounts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import ru.yandex.practicum.mybank.accounts.domain.EventType;
import ru.yandex.practicum.mybank.notifications.outbox.NotificationDeliveryException;
import ru.yandex.practicum.mybank.notifications.outbox.NotificationRequest;
import ru.yandex.practicum.mybank.notifications.outbox.NotificationsOutboxRelay;
import ru.yandex.practicum.mybank.notifications.outbox.NotificationsOutboxProperties;

import java.net.ConnectException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationsOutboxRelayIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private NotificationsOutboxRelay notificationsOutboxRelay;

	@Autowired
	private NotificationsOutboxProperties notificationsOutboxProperties;

	@BeforeEach
	void updateProfile() throws Exception {
		mockMvc.perform(put("/api/customers/me")
						.with(jwt().jwt(jwt -> jwt
								.claim("preferred_username", "user1")
								.claim("scope", "customer:write")))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "user1_new_first_name user1_new_last_name", "birthdate": "1990-01-15"}
								"""))
				.andExpect(status().isOk());
	}

	@Test
	void sendsClaimedEventAndMarksItProcessed() {
		UUID eventUuid = (UUID) event().get("uuid");

		notificationsOutboxRelay.relayPending();

		ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
		verify(notificationsClient).send(captor.capture());

		NotificationRequest sent = captor.getValue();
		assertThat(sent.eventUuid()).isEqualTo(eventUuid);
		assertThat(sent.type()).isEqualTo(EventType.PROFILE_UPDATED.name().toLowerCase(Locale.ROOT));
		assertThat(sent.recipientUuid()).isEqualTo(UUID.fromString(
				jdbcTemplate.queryForObject("select uuid from customers where login = 'user1'", String.class)));
		assertThat(sent.payload()).contains("user1_new_first_name user1_new_last_name");

		Map<String, Object> processed = event();
		assertThat(processed).containsEntry("status", "PROCESSED").containsEntry("attempts", 0);
		assertThat(processed.get("processed_at")).isNotNull();
		assertThat(processed.get("locked_at")).isNull();
	}

	@Test
	void returnsEventToPendingWhenDeliveryFails() {
		doThrow(deliveryFailure()).when(notificationsClient).send(any());

		notificationsOutboxRelay.relayPending();

		Map<String, Object> pending = event();
		assertThat(pending).containsEntry("status", "PENDING").containsEntry("attempts", 1);
		assertThat(pending.get("last_error")).isEqualTo("java.net.ConnectException: Connection refused");
		assertThat(pending.get("locked_at")).isNull();
	}

	@Test
	void failsEventOnceAttemptsAreSpent() {
		doThrow(deliveryFailure()).when(notificationsClient).send(any());

		for (int attempt = 0; attempt < notificationsOutboxProperties.maxAttempts(); attempt++) {
			notificationsOutboxRelay.relayPending();
		}

		assertThat(event())
				.containsEntry("status", "FAILED")
				.containsEntry("attempts", notificationsOutboxProperties.maxAttempts());
	}

	private NotificationDeliveryException deliveryFailure() {
		return new NotificationDeliveryException(UUID.randomUUID(), new ConnectException("Connection refused"));
	}

	private Map<String, Object> event() {
		return jdbcTemplate.queryForMap("""
				select uuid, status, attempts, last_error, locked_at, processed_at from notifications_outbox
				""");
	}
}
