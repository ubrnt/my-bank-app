package ru.yandex.practicum.mybank.accounts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.mybank.accounts.config.OutboxProperties;
import ru.yandex.practicum.mybank.accounts.domain.AggregateType;
import ru.yandex.practicum.mybank.accounts.domain.EventType;
import ru.yandex.practicum.mybank.accounts.domain.OutboxEvent;
import ru.yandex.practicum.mybank.accounts.domain.OutboxStatus;
import ru.yandex.practicum.mybank.accounts.repository.OutboxEventRepository;
import ru.yandex.practicum.mybank.accounts.service.dto.CustomerDto;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

	private static final UUID CUSTOMER_UUID = UUID.fromString("3f2a77c4-1e08-4a6b-8f21-9c0d5b7e1111");
	private static final String ERROR = "java.net.ConnectException: Connection refused";
	private static final int MAX_ATTEMPTS = 5;

	@Mock
	private OutboxEventRepository outboxEventRepository;

	private OutboxService outboxService;

	@BeforeEach
	void setUp() {
		OutboxProperties properties = new OutboxProperties(
				Duration.ofSeconds(5), 20, Duration.ofMinutes(5), MAX_ATTEMPTS);

		outboxService = new OutboxService(outboxEventRepository, JsonMapper.builder().build(), properties);
	}

	@Test
	void savesEvent() {
		outboxService.save(EventType.PROFILE_UPDATED, AggregateType.CUSTOMER, 7L,
				CUSTOMER_UUID,
				new CustomerDto(CUSTOMER_UUID, "user1", "user1_first_name user1_last_name"));

		ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(outboxEventRepository).save(captor.capture());

		OutboxEvent saved = captor.getValue();
		assertThat(saved.getEventType()).isEqualTo(EventType.PROFILE_UPDATED);
		assertThat(saved.getAggregateType()).isEqualTo(AggregateType.CUSTOMER);
		assertThat(saved.getAggregateId()).isEqualTo(7L);
		assertThat(saved.getRecipientUuid()).isEqualTo(CUSTOMER_UUID);
		assertThat(saved.getPayload()).isEqualTo(
				"{\"uuid\":\"3f2a77c4-1e08-4a6b-8f21-9c0d5b7e1111\",\"login\":\"user1\",\"name\":\"user1_first_name user1_last_name\"}");
	}

	@Test
	void retriesWhileAttemptsAreLeft() {
		OutboxEvent event = eventWithFailedAttempts(0);
		when(outboxEventRepository.findById(anyLong())).thenReturn(Optional.of(event));

		outboxService.markNotDelivered(1L, ERROR);

		assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
		assertThat(event.getAttempts()).isEqualTo(1);
		assertThat(event.getLastError()).isEqualTo(ERROR);
	}

	@Test
	void failsOnceMaxAttemptsReached() {
		OutboxEvent event = eventWithFailedAttempts(MAX_ATTEMPTS - 1);
		when(outboxEventRepository.findById(anyLong())).thenReturn(Optional.of(event));

		outboxService.markNotDelivered(1L, ERROR);

		assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
		assertThat(event.getAttempts()).isEqualTo(MAX_ATTEMPTS);
	}

	@Test
	void marksProcessed() {
		OutboxEvent event = eventWithFailedAttempts(0);
		when(outboxEventRepository.findById(anyLong())).thenReturn(Optional.of(event));

		outboxService.markProcessed(1L);

		assertThat(event.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
		assertThat(event.getProcessedAt()).isNotNull();
	}

	private OutboxEvent eventWithFailedAttempts(int attempts) {
		OutboxEvent event = new OutboxEvent(EventType.PROFILE_UPDATED, AggregateType.CUSTOMER, 7L,
				CUSTOMER_UUID, "{\"uuid\":\"cccc\"}");

		for (int attempt = 0; attempt < attempts; attempt++) {
			event.markPending(ERROR);
		}

		return event;
	}
}
