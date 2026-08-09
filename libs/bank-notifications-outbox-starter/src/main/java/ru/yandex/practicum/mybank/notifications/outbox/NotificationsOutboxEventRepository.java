package ru.yandex.practicum.mybank.notifications.outbox;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

public class NotificationsOutboxEventRepository {

	private static final String CLAIM_SQL = """
			update notifications_outbox
			   set status = 'PROCESSING', locked_at = now()
			 where id in (
			       select id from notifications_outbox
			        where (status = 'PENDING' and next_attempt_at <= now())
			           or (status = 'PROCESSING' and locked_at < now() - make_interval(secs => :staleTimeoutSeconds))
			        order by id
			        limit :batchSize
			        for update skip locked
			 )
			returning *
			""";

	@PersistenceContext
	private EntityManager entityManager;

	public void save(NotificationsOutboxEvent event) {
		entityManager.persist(event);
	}

	@SuppressWarnings("unchecked")
	public List<NotificationsOutboxEvent> claim(long staleTimeoutSeconds, int batchSize) {
		return entityManager.createNativeQuery(CLAIM_SQL, NotificationsOutboxEvent.class)
				.setParameter("staleTimeoutSeconds", staleTimeoutSeconds)
				.setParameter("batchSize", batchSize)
				.getResultList();
	}

	public Optional<NotificationsOutboxEvent> findById(long id) {
		return Optional.ofNullable(entityManager.find(NotificationsOutboxEvent.class, id));
	}
}
