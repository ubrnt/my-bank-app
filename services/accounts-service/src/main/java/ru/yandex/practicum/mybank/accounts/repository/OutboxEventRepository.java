package ru.yandex.practicum.mybank.accounts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.mybank.accounts.domain.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
}
