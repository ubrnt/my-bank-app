package ru.yandex.practicum.mybank.accounts.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.mybank.accounts.PostgresContainerConfig;
import ru.yandex.practicum.mybank.accounts.config.JpaConfig;
import ru.yandex.practicum.mybank.accounts.domain.AggregateType;
import ru.yandex.practicum.mybank.accounts.domain.EventType;
import ru.yandex.practicum.mybank.accounts.domain.OutboxEvent;
import ru.yandex.practicum.mybank.accounts.domain.OutboxStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresContainerConfig.class, JpaConfig.class})
class OutboxEventRepositoryTest {

	private static final long STALE_TIMEOUT_SECONDS = 300;
	private static final int BATCH_SIZE = 2;

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void claimsNoMoreThanBatchSize() {
		saveEvent();
		saveEvent();
		saveEvent();
		flushAndClear();

		List<OutboxEvent> claimed = outboxEventRepository.claim(STALE_TIMEOUT_SECONDS, BATCH_SIZE);

		assertThat(claimed).hasSize(BATCH_SIZE).allSatisfy(event -> {
			assertThat(event.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
			assertThat(event.getLockedAt()).isNotNull();
		});
		assertThat(outboxEventRepository.findAll())
				.filteredOn(event -> event.getStatus() == OutboxStatus.PENDING)
				.hasSize(1);
	}

	@Test
	void skipsEventsClaimedBySomeoneElse() {
		OutboxEvent event = saveEvent();
		flushAndClear();
		outboxEventRepository.claim(STALE_TIMEOUT_SECONDS, BATCH_SIZE);
		flushAndClear();

		List<OutboxEvent> claimed = outboxEventRepository.claim(STALE_TIMEOUT_SECONDS, BATCH_SIZE);

		assertThat(claimed).isEmpty();
		assertThat(outboxEventRepository.findById(event.getId()).orElseThrow().getStatus())
				.isEqualTo(OutboxStatus.PROCESSING);
	}

	@Test
	void claimsEventsWhoseLockHasExpired() {
		OutboxEvent event = saveEvent();
		flushAndClear();
		outboxEventRepository.claim(STALE_TIMEOUT_SECONDS, BATCH_SIZE);
		expireLock(event.getId());

		List<OutboxEvent> claimed = outboxEventRepository.claim(STALE_TIMEOUT_SECONDS, BATCH_SIZE);

		assertThat(claimed).extracting(OutboxEvent::getId).containsExactly(event.getId());
	}

	@Test
	void releasesLockWhenProcessed() {
		saveEvent();
		flushAndClear();

		OutboxEvent claimed = outboxEventRepository.claim(STALE_TIMEOUT_SECONDS, BATCH_SIZE).getFirst();
		claimed.markProcessed();
		flushAndClear();
		flushAndClear();

		OutboxEvent reloaded = outboxEventRepository.findById(claimed.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
		assertThat(reloaded.getProcessedAt()).isNotNull();
		assertThat(reloaded.getLockedAt()).isNull();
	}

	private OutboxEvent saveEvent() {
		return outboxEventRepository.save(new OutboxEvent(EventType.PROFILE_UPDATED, AggregateType.CUSTOMER, 7L,
				UUID.randomUUID(), "{\"uuid\":\"cccc0001-2222-4333-8444-555566660003\"}"));
	}

	private void expireLock(long id) {
		entityManager.createNativeQuery("""
						update outbox_events
						   set locked_at = now() - :staleTimeoutSeconds * interval '2 second'
						 where id = :id
						""")
				.setParameter("staleTimeoutSeconds", STALE_TIMEOUT_SECONDS)
				.setParameter("id", id)
				.executeUpdate();
		flushAndClear();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
