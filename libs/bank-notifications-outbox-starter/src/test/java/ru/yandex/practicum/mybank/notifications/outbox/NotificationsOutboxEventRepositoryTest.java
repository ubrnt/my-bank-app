package ru.yandex.practicum.mybank.notifications.outbox;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresContainerConfig.class, NotificationsOutboxEventRepository.class})
class NotificationsOutboxEventRepositoryTest {

	private static final long STALE_TIMEOUT_SECONDS = 300;
	private static final int BATCH_SIZE = 2;

	@Autowired
	private NotificationsOutboxEventRepository notificationsOutboxEventRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void claimsNoMoreThanBatchSize() {
		saveEvent();
		saveEvent();
		saveEvent();
		flushAndClear();

		List<NotificationsOutboxEvent> claimed = notificationsOutboxEventRepository.claim(STALE_TIMEOUT_SECONDS, BATCH_SIZE);

		assertThat(claimed).hasSize(BATCH_SIZE).allSatisfy(event -> {
			assertThat(event.getStatus()).isEqualTo(NotificationsOutboxStatus.PROCESSING);
			assertThat(event.getLockedAt()).isNotNull();
		});
		assertThat(countWithStatus(NotificationsOutboxStatus.PENDING)).isEqualTo(1);
	}

	@Test
	void skipsEventsClaimedBySomeoneElse() {
		NotificationsOutboxEvent event = saveEvent();
		flushAndClear();
		notificationsOutboxEventRepository.claim(STALE_TIMEOUT_SECONDS, BATCH_SIZE);
		flushAndClear();

		List<NotificationsOutboxEvent> claimed = notificationsOutboxEventRepository.claim(STALE_TIMEOUT_SECONDS, BATCH_SIZE);

		assertThat(claimed).isEmpty();
		assertThat(notificationsOutboxEventRepository.findById(event.getId()).orElseThrow().getStatus())
				.isEqualTo(NotificationsOutboxStatus.PROCESSING);
	}

	@Test
	void claimsEventsWhoseLockHasExpired() {
		NotificationsOutboxEvent event = saveEvent();
		flushAndClear();
		notificationsOutboxEventRepository.claim(STALE_TIMEOUT_SECONDS, BATCH_SIZE);
		expireLock(event.getId());

		List<NotificationsOutboxEvent> claimed = notificationsOutboxEventRepository.claim(STALE_TIMEOUT_SECONDS, BATCH_SIZE);

		assertThat(claimed).extracting(NotificationsOutboxEvent::getId).containsExactly(event.getId());
	}

	@Test
	void releasesLockWhenProcessed() {
		saveEvent();
		flushAndClear();

		NotificationsOutboxEvent claimed = notificationsOutboxEventRepository.claim(STALE_TIMEOUT_SECONDS, BATCH_SIZE).getFirst();
		claimed.markProcessed();
		flushAndClear();

		NotificationsOutboxEvent reloaded = notificationsOutboxEventRepository.findById(claimed.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(NotificationsOutboxStatus.PROCESSED);
		assertThat(reloaded.getProcessedAt()).isNotNull();
		assertThat(reloaded.getLockedAt()).isNull();
	}

	private NotificationsOutboxEvent saveEvent() {
		NotificationsOutboxEvent event = new NotificationsOutboxEvent("PROFILE_UPDATED", "CUSTOMER", 7L,
				UUID.randomUUID(), "{\"uuid\":\"cccc0001-2222-4333-8444-555566660003\"}");
		notificationsOutboxEventRepository.save(event);

		return event;
	}

	private long countWithStatus(NotificationsOutboxStatus status) {
		return entityManager
				.createQuery("select count(e) from NotificationsOutboxEvent e where e.status = :status", Long.class)
				.setParameter("status", status)
				.getSingleResult();
	}

	private void expireLock(long id) {
		entityManager.createNativeQuery("""
						update notifications_outbox
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
