package ru.yandex.practicum.mybank.notifications.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.ConnectException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationsOutboxRelayTest {

	private static final UUID RECIPIENT_UUID = UUID.fromString("3f2a77c4-1e08-4a6b-8f21-9c0d5b7e1111");
	private static final String PAYLOAD_JSON = "{\"uuid\":\"cccc0001-2222-4333-8444-555566660003\"}";

	@Mock
	private NotificationsOutboxService notificationsOutboxService;

	@Mock
	private NotificationsClient notificationsClient;

	@InjectMocks
	private NotificationsOutboxRelay notificationsOutboxRelay;

	@Test
	void doesNotCallNotificationsWhenNothingClaimed() {
		when(notificationsOutboxService.claim()).thenReturn(List.of());

		notificationsOutboxRelay.relayPending();

		verifyNoInteractions(notificationsClient);
	}

	@Test
	void lowercasesTypeAndSendsOtherColumnsAsIs() {
		NotificationsOutboxEvent event = claimed(1L);
		when(notificationsOutboxService.claim()).thenReturn(List.of(event));

		notificationsOutboxRelay.relayPending();

		ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
		verify(notificationsClient).send(captor.capture());

		NotificationRequest request = captor.getValue();
		assertThat(request.eventUuid()).isEqualTo(event.getUuid());
		assertThat(request.type()).isEqualTo("customer_updated");
		assertThat(request.recipientUuid()).isEqualTo(RECIPIENT_UUID);
		assertThat(request.payload()).isEqualTo(PAYLOAD_JSON);

		verify(notificationsOutboxService).markProcessed(1L);
	}

	@Test
	void recordsFailureWhenDeliveryFails() {
		NotificationsOutboxEvent event = claimed(1L);
		when(notificationsOutboxService.claim()).thenReturn(List.of(event));
		doThrow(new NotificationDeliveryException(event.getUuid(), new ConnectException("Connection refused")))
				.when(notificationsClient).send(any());

		notificationsOutboxRelay.relayPending();

		verify(notificationsOutboxService).markNotDelivered(1L, "java.net.ConnectException: Connection refused");
		verify(notificationsOutboxService, never()).markProcessed(1L);
	}

	@Test
	void keepsProcessingBatchAfterOneFailure() {
		NotificationsOutboxEvent failing = claimed(1L);
		NotificationsOutboxEvent succeeding = claimed(2L);
		when(notificationsOutboxService.claim()).thenReturn(List.of(failing, succeeding));
		doThrow(new NotificationDeliveryException(failing.getUuid(), new ConnectException("Connection refused")))
				.doNothing()
				.when(notificationsClient).send(any());

		notificationsOutboxRelay.relayPending();

		verify(notificationsOutboxService).markNotDelivered(1L, "java.net.ConnectException: Connection refused");
		verify(notificationsOutboxService).markProcessed(2L);
	}

	private NotificationsOutboxEvent claimed(long id) {
		NotificationsOutboxEvent event = new NotificationsOutboxEvent(
				"CUSTOMER_UPDATED", "CUSTOMER", 7L, RECIPIENT_UUID, PAYLOAD_JSON);
		ReflectionTestUtils.setField(event, "id", id);

		return event;
	}
}
