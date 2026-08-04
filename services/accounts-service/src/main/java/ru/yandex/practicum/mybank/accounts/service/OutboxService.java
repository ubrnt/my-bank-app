package ru.yandex.practicum.mybank.accounts.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.mybank.accounts.domain.AggregateType;
import ru.yandex.practicum.mybank.accounts.domain.EventType;
import ru.yandex.practicum.mybank.accounts.domain.OutboxEvent;
import ru.yandex.practicum.mybank.accounts.repository.OutboxEventRepository;
import tools.jackson.databind.ObjectMapper;

@Service
public class OutboxService {

	private final OutboxEventRepository outboxEventRepository;
	private final ObjectMapper objectMapper;

	public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
		this.outboxEventRepository = outboxEventRepository;
		this.objectMapper = objectMapper;
	}

	public void save(EventType eventType, AggregateType aggregateType, long aggregateId, Object payload) {
		String json = objectMapper.writeValueAsString(payload);

		outboxEventRepository.save(new OutboxEvent(eventType, aggregateType, aggregateId, json));
	}
}
