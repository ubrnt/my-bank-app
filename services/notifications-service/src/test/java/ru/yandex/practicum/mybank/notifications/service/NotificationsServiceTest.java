package ru.yandex.practicum.mybank.notifications.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.yandex.practicum.mybank.notifications.client.AccountsClient;
import ru.yandex.practicum.mybank.notifications.client.CustomerResolutionException;
import ru.yandex.practicum.mybank.notifications.client.UnknownRecipientException;
import ru.yandex.practicum.mybank.notifications.client.dto.CustomerResponse;
import ru.yandex.practicum.mybank.notifications.domain.EventType;
import ru.yandex.practicum.mybank.notifications.domain.Notification;
import ru.yandex.practicum.mybank.notifications.repository.NotificationRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.ConnectException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationsServiceTest {

	private static final UUID EVENT_UUID = UUID.fromString("1b7f4a90-0d51-4c2e-9f77-0a1e5c3b0001");
	private static final UUID RECIPIENT_UUID = UUID.fromString("3f2a77c4-1e08-4a6b-8f21-9c0d5b7e1111");
	private static final String MESSAGE = "Счёт *0001: пополнение на 5000. Доступно 105000";

	private final JsonNode payload = JsonMapper.builder().build().readTree("""
			{"uuid": "cccc0001-2222-4333-8444-555566660001", "type": "DEPOSIT"}
			""");

	private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
	private final MessageRenderer messageRenderer = mock(MessageRenderer.class);
	private final AccountsClient accountsClient = mock(AccountsClient.class);
	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
	private final NotificationsService notificationsService = new NotificationsService(
			notificationRepository, messageRenderer, accountsClient, new NotificationsMetrics(meterRegistry));

	@Test
	void resolvesRecipientRendersAndSaves() {
		when(accountsClient.getCustomer(RECIPIENT_UUID))
				.thenReturn(new CustomerResponse("user1", "Иванов Иван"));
		when(messageRenderer.render(EventType.MONEY_DEPOSITED, payload)).thenReturn(MESSAGE);
		when(notificationRepository.insertIfAbsent(any(), any(), any(), anyString(), anyString(), anyString()))
				.thenReturn(Optional.of(notification()));

		notificationsService.receive(EVENT_UUID, EventType.MONEY_DEPOSITED, RECIPIENT_UUID, payload);

		ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
		verify(notificationRepository).insertIfAbsent(any(UUID.class), eq(EVENT_UUID), eq(RECIPIENT_UUID),
				eq("MONEY_DEPOSITED"), payloadCaptor.capture(), eq(MESSAGE));
		assertThat(payloadCaptor.getValue()).contains("cccc0001-2222-4333-8444-555566660001");
	}

	@Test
	void skipsEventAlreadyNotifiedAbout() {
		when(notificationRepository.existsByEventUuid(EVENT_UUID)).thenReturn(true);

		notificationsService.receive(EVENT_UUID, EventType.MONEY_DEPOSITED, RECIPIENT_UUID, payload);

		verifyNoInteractions(accountsClient, messageRenderer);
		verify(notificationRepository, never()).insertIfAbsent(any(), any(), any(), anyString(), anyString(),
				anyString());
	}

	@Test
	void failsWhenRecipientCannotBeResolved() {
		when(accountsClient.getCustomer(RECIPIENT_UUID))
				.thenThrow(new CustomerResolutionException(RECIPIENT_UUID, new ConnectException("refused")));

		assertThatThrownBy(() -> notificationsService.receive(
				EVENT_UUID, EventType.MONEY_DEPOSITED, RECIPIENT_UUID, payload))
				.isInstanceOf(CustomerResolutionException.class);

		verify(notificationRepository, never()).insertIfAbsent(any(), any(), any(), anyString(), anyString(),
				anyString());
		assertThat(meterRegistry.find(NotificationsMetrics.DELIVERY_FAILURES).counter()).isNull();
	}

	@Test
	void countsDeliveryToUnknownRecipient() {
		when(accountsClient.getCustomer(RECIPIENT_UUID))
				.thenThrow(new UnknownRecipientException(RECIPIENT_UUID, new RuntimeException("404")));

		assertThatThrownBy(() -> notificationsService.receive(
				EVENT_UUID, EventType.MONEY_DEPOSITED, RECIPIENT_UUID, payload))
				.isInstanceOf(UnknownRecipientException.class);

		assertThat(deliveryFailures("unknown_recipient")).isEqualTo(1.0);
	}

	@Test
	void countsUndeliverableEvent() {
		when(accountsClient.getCustomer(RECIPIENT_UUID))
				.thenReturn(new CustomerResponse("user1", "Иванов Иван"));
		when(messageRenderer.render(EventType.MONEY_DEPOSITED, payload))
				.thenThrow(new InvalidEventException("Event payload lacks field 'amount'"));

		assertThatThrownBy(() -> notificationsService.receive(
				EVENT_UUID, EventType.MONEY_DEPOSITED, RECIPIENT_UUID, payload))
				.isInstanceOf(InvalidEventException.class);

		assertThat(deliveryFailures("invalid_event")).isEqualTo(1.0);
	}

	private double deliveryFailures(String reason) {
		return meterRegistry.get(NotificationsMetrics.DELIVERY_FAILURES)
				.tags("reason", reason)
				.counter()
				.count();
	}

	private Notification notification() {
		return new Notification(EVENT_UUID, RECIPIENT_UUID, EventType.MONEY_DEPOSITED, payload.toString(), MESSAGE);
	}
}
