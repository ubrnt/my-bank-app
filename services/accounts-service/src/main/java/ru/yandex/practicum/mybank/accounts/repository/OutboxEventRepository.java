package ru.yandex.practicum.mybank.accounts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.mybank.accounts.domain.OutboxEvent;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

	@Query(value = """
			update outbox_events
			   set status = 'PROCESSING', locked_at = now()
			 where id in (
			       select id from outbox_events
			        where status = 'PENDING'
			           or (status = 'PROCESSING' and locked_at < now() - :staleTimeoutSeconds * interval '1 second')
			        order by id
			        limit :batchSize
			        for update skip locked
			 )
			returning *
			""", nativeQuery = true)
	List<OutboxEvent> claim(long staleTimeoutSeconds, int batchSize);
}
