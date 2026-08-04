package ru.yandex.practicum.mybank.accounts.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.practicum.mybank.accounts.client.NotificationDeliveryException;
import ru.yandex.practicum.mybank.accounts.client.NotificationsClient;
import ru.yandex.practicum.mybank.accounts.client.dto.NotificationRequest;
import ru.yandex.practicum.mybank.accounts.domain.AggregateType;
import ru.yandex.practicum.mybank.accounts.domain.EventType;
import ru.yandex.practicum.mybank.accounts.domain.OutboxEvent;

import java.net.ConnectException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

	private static final String RECIPIENT_JSON = "{\"uuid\":\"3f2a77c4-1e08-4a6b-8f21-9c0d5b7e1111\",\"login\":\"user1\"}";
	private static final String PAYLOAD_JSON = "{\"uuid\":\"cccc0001-2222-4333-8444-555566660003\"}";

	@Mock
	private OutboxService outboxService;

	@Mock
	private NotificationsClient notificationsClient;

	@InjectMocks
	private OutboxRelay outboxRelay;

	@Test
	void doesNotCallNotificationsWhenNothingClaimed() {
		when(outboxService.claim()).thenReturn(List.of());

		outboxRelay.relayPending();

		verifyNoInteractions(notificationsClient);
	}

	@Test
	void sendsColumnsOfClaimedEventAsIs() {
		OutboxEvent event = claimed(1L);
		when(outboxService.claim()).thenReturn(List.of(event));

		outboxRelay.relayPending();

		ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
		verify(notificationsClient).send(captor.capture());

		NotificationRequest request = captor.getValue();
		assertThat(request.eventUuid()).isEqualTo(event.getUuid());
		assertThat(request.type()).isEqualTo(EventType.MONEY_SENT);
		assertThat(request.recipient()).isEqualTo(RECIPIENT_JSON);
		assertThat(request.payload()).isEqualTo(PAYLOAD_JSON);

		verify(outboxService).markProcessed(1L);
	}

	@Test
	void recordsFailureWhenDeliveryFails() {
		OutboxEvent event = claimed(1L);
		when(outboxService.claim()).thenReturn(List.of(event));
		doThrow(new NotificationDeliveryException(event.getUuid(), new ConnectException("Connection refused")))
				.when(notificationsClient).send(any());

		outboxRelay.relayPending();

		verify(outboxService).markNotDelivered(1L, "java.net.ConnectException: Connection refused");
		verify(outboxService, org.mockito.Mockito.never()).markProcessed(1L);
	}

	@Test
	void keepsProcessingBatchAfterOneFailure() {
		OutboxEvent failing = claimed(1L);
		OutboxEvent succeeding = claimed(2L);
		when(outboxService.claim()).thenReturn(List.of(failing, succeeding));
		doThrow(new NotificationDeliveryException(failing.getUuid(), new ConnectException("Connection refused")))
				.doNothing()
				.when(notificationsClient).send(any());

		outboxRelay.relayPending();

		verify(outboxService).markNotDelivered(1L, "java.net.ConnectException: Connection refused");
		verify(outboxService).markProcessed(2L);
	}

	private OutboxEvent claimed(long id) {
		OutboxEvent event = new OutboxEvent(
				EventType.MONEY_SENT, AggregateType.TRANSACTION, 41L, RECIPIENT_JSON, PAYLOAD_JSON);
		ReflectionTestUtils.setField(event, "id", id);

		return event;
	}
}
