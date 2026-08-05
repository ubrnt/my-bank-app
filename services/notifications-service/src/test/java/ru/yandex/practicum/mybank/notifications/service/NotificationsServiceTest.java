package ru.yandex.practicum.mybank.notifications.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.mybank.notifications.client.AccountsClient;
import ru.yandex.practicum.mybank.notifications.client.CustomerResolutionException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationsServiceTest {

	private static final UUID EVENT_UUID = UUID.fromString("1b7f4a90-0d51-4c2e-9f77-0a1e5c3b0001");
	private static final UUID RECIPIENT_UUID = UUID.fromString("3f2a77c4-1e08-4a6b-8f21-9c0d5b7e1111");
	private static final String MESSAGE = "Счёт *0001: пополнение на 5000. Доступно 105000";

	private final JsonNode payload = JsonMapper.builder().build().readTree("""
			{"uuid": "cccc0001-2222-4333-8444-555566660001", "type": "DEPOSIT"}
			""");

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private MessageRenderer messageRenderer;

	@Mock
	private AccountsClient accountsClient;

	@InjectMocks
	private NotificationsService notificationsService;

	@Test
	void resolvesRecipientRendersAndSaves() {
		when(accountsClient.getCustomer(RECIPIENT_UUID))
				.thenReturn(new CustomerResponse("user1", "user1_first_name user1_last_name"));
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
	void acceptsDuplicateSilently() {
		when(accountsClient.getCustomer(RECIPIENT_UUID))
				.thenReturn(new CustomerResponse("user1", "user1_first_name user1_last_name"));
		when(messageRenderer.render(any(), any())).thenReturn(MESSAGE);
		when(notificationRepository.insertIfAbsent(any(), any(), any(), anyString(), anyString(), anyString()))
				.thenReturn(Optional.empty());

		notificationsService.receive(EVENT_UUID, EventType.MONEY_DEPOSITED, RECIPIENT_UUID, payload);
	}

	@Test
	void failsWhenRecipientCannotBeResolved() {
		when(accountsClient.getCustomer(RECIPIENT_UUID))
				.thenThrow(new CustomerResolutionException(RECIPIENT_UUID, new ConnectException("refused")));

		assertThatThrownBy(() -> notificationsService.receive(
				EVENT_UUID, EventType.MONEY_DEPOSITED, RECIPIENT_UUID, payload))
				.isInstanceOf(CustomerResolutionException.class);

		verifyNoInteractions(notificationRepository);
	}

	private Notification notification() {
		return new Notification(EVENT_UUID, RECIPIENT_UUID, EventType.MONEY_DEPOSITED, payload.toString(), MESSAGE);
	}
}
