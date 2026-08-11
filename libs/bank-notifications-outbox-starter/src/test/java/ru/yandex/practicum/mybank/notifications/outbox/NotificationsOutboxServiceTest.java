package ru.yandex.practicum.mybank.notifications.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationsOutboxServiceTest {

	private static final UUID CUSTOMER_UUID = UUID.fromString("3f2a77c4-1e08-4a6b-8f21-9c0d5b7e1111");
	private static final String ERROR = "java.net.ConnectException: Connection refused";
	private static final int MAX_ATTEMPTS = 8;
	private static final Duration RETRY_DELAY = Duration.ofSeconds(5);
	private static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(1);
	private static final Instant NOW = Instant.parse("2026-08-09T12:00:00Z");

	private record CustomerPayload(UUID uuid, String login, String name) {
	}

	@Mock
	private NotificationsOutboxEventRepository notificationsOutboxEventRepository;

	private NotificationsOutboxService notificationsOutboxService;

	@BeforeEach
	void setUp() {
		NotificationsOutboxProperties properties = new NotificationsOutboxProperties(
				Duration.ofSeconds(5), 20, Duration.ofMinutes(5), MAX_ATTEMPTS, RETRY_DELAY, MAX_RETRY_DELAY);

		notificationsOutboxService = new NotificationsOutboxService(notificationsOutboxEventRepository, JsonMapper.builder().build(), properties);
	}

	@Test
	void savesEvent() {
		notificationsOutboxService.save("CUSTOMER_UPDATED", "CUSTOMER", 7L, CUSTOMER_UUID,
				new CustomerPayload(CUSTOMER_UUID, "user1", "user1_first_name user1_last_name"));

		ArgumentCaptor<NotificationsOutboxEvent> captor = ArgumentCaptor.forClass(NotificationsOutboxEvent.class);
		verify(notificationsOutboxEventRepository).save(captor.capture());

		NotificationsOutboxEvent saved = captor.getValue();
		assertThat(saved.getEventType()).isEqualTo("CUSTOMER_UPDATED");
		assertThat(saved.getAggregateType()).isEqualTo("CUSTOMER");
		assertThat(saved.getAggregateId()).isEqualTo(7L);
		assertThat(saved.getRecipientUuid()).isEqualTo(CUSTOMER_UUID);
		assertThat(saved.getPayload()).isEqualTo(
				"{\"uuid\":\"3f2a77c4-1e08-4a6b-8f21-9c0d5b7e1111\",\"login\":\"user1\",\"name\":\"user1_first_name user1_last_name\"}");
	}

	@Test
	void retriesWhileAttemptsAreLeft() {
		NotificationsOutboxEvent event = eventWithFailedAttempts(0);
		when(notificationsOutboxEventRepository.findById(anyLong())).thenReturn(Optional.of(event));
		when(notificationsOutboxEventRepository.currentTimestamp()).thenReturn(NOW);

		notificationsOutboxService.markNotDelivered(1L, ERROR);

		assertThat(event.getStatus()).isEqualTo(NotificationsOutboxStatus.PENDING);
		assertThat(event.getAttempts()).isEqualTo(1);
		assertThat(event.getLastError()).isEqualTo(ERROR);
	}

	@Test
	void postponesEveryRetryFurtherThanThePrevious() {
		NotificationsOutboxEvent event = eventWithFailedAttempts(2);
		when(notificationsOutboxEventRepository.findById(anyLong())).thenReturn(Optional.of(event));
		when(notificationsOutboxEventRepository.currentTimestamp()).thenReturn(NOW);

		notificationsOutboxService.markNotDelivered(1L, ERROR);

		assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plus(RETRY_DELAY.multipliedBy(4)));
	}

	@Test
	void doesNotPostponeRetriesBeyondTheCap() {
		NotificationsOutboxEvent event = eventWithFailedAttempts(5);
		when(notificationsOutboxEventRepository.findById(anyLong())).thenReturn(Optional.of(event));
		when(notificationsOutboxEventRepository.currentTimestamp()).thenReturn(NOW);

		notificationsOutboxService.markNotDelivered(1L, ERROR);

		assertThat(RETRY_DELAY.multipliedBy(32)).isGreaterThan(MAX_RETRY_DELAY);
		assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plus(MAX_RETRY_DELAY));
	}

	@Test
	void failsOnceMaxAttemptsReached() {
		NotificationsOutboxEvent event = eventWithFailedAttempts(MAX_ATTEMPTS - 1);
		when(notificationsOutboxEventRepository.findById(anyLong())).thenReturn(Optional.of(event));

		notificationsOutboxService.markNotDelivered(1L, ERROR);

		assertThat(event.getStatus()).isEqualTo(NotificationsOutboxStatus.FAILED);
		assertThat(event.getAttempts()).isEqualTo(MAX_ATTEMPTS);
	}

	@Test
	void marksProcessed() {
		NotificationsOutboxEvent event = eventWithFailedAttempts(0);
		when(notificationsOutboxEventRepository.findById(anyLong())).thenReturn(Optional.of(event));

		notificationsOutboxService.markProcessed(1L);

		assertThat(event.getStatus()).isEqualTo(NotificationsOutboxStatus.PROCESSED);
		assertThat(event.getProcessedAt()).isNotNull();
	}

	private NotificationsOutboxEvent eventWithFailedAttempts(int attempts) {
		NotificationsOutboxEvent event = new NotificationsOutboxEvent("CUSTOMER_UPDATED", "CUSTOMER", 7L,
				CUSTOMER_UUID, "{\"uuid\":\"cccc\"}");

		for (int attempt = 0; attempt < attempts; attempt++) {
			event.markPending(ERROR, Instant.now());
		}

		return event;
	}
}
