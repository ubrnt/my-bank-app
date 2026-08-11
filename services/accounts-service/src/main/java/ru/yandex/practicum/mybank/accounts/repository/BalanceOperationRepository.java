package ru.yandex.practicum.mybank.accounts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.mybank.accounts.domain.BalanceOperation;
import ru.yandex.practicum.mybank.accounts.domain.OperationDirection;
import ru.yandex.practicum.mybank.accounts.domain.Transaction;

import java.util.List;

public interface BalanceOperationRepository extends JpaRepository<BalanceOperation, Long> {

	@Query("""
			select o
			from BalanceOperation o
			join fetch o.account a
			join fetch a.customer c
			where o.transaction = :transaction
				and o.direction = :direction
				and c.login = :login
				and o.amount = :amount
			""")
	List<BalanceOperation> findMatchingOperations(Transaction transaction, OperationDirection direction, String login,
			long amount);

	@Query("""
			select o
			from BalanceOperation o
			join fetch o.account a
			join fetch a.customer c
			where o.transaction = :transaction
				and o.amount = :amount
				and ((o.direction = :withdrawDirection and c.login = :fromLogin)
					or (o.direction = :depositDirection and c.login = :toLogin))
			order by o.id
			""")
	List<BalanceOperation> findMatchingTransferOperations(Transaction transaction, OperationDirection withdrawDirection,
			String fromLogin, OperationDirection depositDirection, String toLogin, long amount);
}
