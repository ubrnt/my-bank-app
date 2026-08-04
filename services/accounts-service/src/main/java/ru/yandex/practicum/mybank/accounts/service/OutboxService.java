package ru.yandex.practicum.mybank.accounts.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mybank.accounts.config.OutboxProperties;
import ru.yandex.practicum.mybank.accounts.domain.AggregateType;
import ru.yandex.practicum.mybank.accounts.domain.EventType;
import ru.yandex.practicum.mybank.accounts.domain.OutboxEvent;
import ru.yandex.practicum.mybank.accounts.repository.OutboxEventRepository;
import ru.yandex.practicum.mybank.accounts.service.dto.RecipientDto;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class OutboxService {

	private final OutboxEventRepository outboxEventRepository;
	private final ObjectMapper objectMapper;
	private final OutboxProperties properties;

	public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper,
			OutboxProperties properties) {
		this.outboxEventRepository = outboxEventRepository;
		this.objectMapper = objectMapper;
		this.properties = properties;
	}

	public void save(EventType eventType, AggregateType aggregateType, long aggregateId, RecipientDto recipient,
			Object payload) {
		String recipientJson = objectMapper.writeValueAsString(recipient);
		String payloadJson = objectMapper.writeValueAsString(payload);

		outboxEventRepository.save(
				new OutboxEvent(eventType, aggregateType, aggregateId, recipientJson, payloadJson));
	}

	@Transactional
	public List<OutboxEvent> claim() {
		return outboxEventRepository.claim(properties.staleTimeout().toSeconds(), properties.batchSize());
	}

	@Transactional
	public void markProcessed(long id) {
		outboxEventRepository.findById(id).orElseThrow().markProcessed();
	}

	@Transactional
	public void markNotDelivered(long id, String error) {
		OutboxEvent event = outboxEventRepository.findById(id).orElseThrow();

		if (event.getAttempts() + 1 >= properties.maxAttempts()) {
			event.markFailed(error);
		} else {
			event.markPending(error);
		}
	}
}
