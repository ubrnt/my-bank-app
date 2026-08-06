package ru.yandex.practicum.mybank.cash;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.mybank.cash.domain.CashOperation;
import ru.yandex.practicum.mybank.cash.domain.CashOperationStatus;
import ru.yandex.practicum.mybank.cash.domain.CashOperationType;
import ru.yandex.practicum.mybank.cash.repository.CashOperationRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresContainerConfig.class)
class CashOperationRepositoryTest {

	@Autowired
	private CashOperationRepository cashOperationRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void persistsPendingOperationWithAuditFields() {
		UUID transactionUuid = UUID.randomUUID();
		cashOperationRepository.save(new CashOperation(transactionUuid, CashOperationType.DEPOSIT, 1000L));
		flushAndClear();

		CashOperation reloaded = cashOperationRepository.findByUuid(transactionUuid).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(CashOperationStatus.PENDING);
		assertThat(reloaded.getType()).isEqualTo(CashOperationType.DEPOSIT);
		assertThat(reloaded.getAmount()).isEqualTo(1000L);
		assertThat(reloaded.getAccountUuid()).isNull();
		assertThat(reloaded.getCustomerUuid()).isNull();
		assertThat(reloaded.getFailureReason()).isNull();
		assertThat(reloaded.getCreatedTs()).isNotNull();
		assertThat(reloaded.getUpdatedTs()).isNotNull();
	}

	@Test
	void completeStoresAccountAndCustomerUuids() {
		CashOperation operation = saveOperation();
		flushAndClear();

		UUID accountUuid = UUID.randomUUID();
		UUID customerUuid = UUID.randomUUID();
		cashOperationRepository.findByUuid(operation.getUuid()).orElseThrow().complete(accountUuid, customerUuid);
		flushAndClear();

		CashOperation completed = cashOperationRepository.findByUuid(operation.getUuid()).orElseThrow();
		assertThat(completed.getStatus()).isEqualTo(CashOperationStatus.COMPLETED);
		assertThat(completed.getAccountUuid()).isEqualTo(accountUuid);
		assertThat(completed.getCustomerUuid()).isEqualTo(customerUuid);
		assertThat(completed.getFailureReason()).isNull();
	}

	@Test
	void failStoresReasonWithoutAccountAndCustomer() {
		CashOperation operation = saveOperation();
		flushAndClear();

		cashOperationRepository.findByUuid(operation.getUuid()).orElseThrow().fail("insufficient_funds");
		flushAndClear();

		CashOperation failed = cashOperationRepository.findByUuid(operation.getUuid()).orElseThrow();
		assertThat(failed.getStatus()).isEqualTo(CashOperationStatus.FAILED);
		assertThat(failed.getFailureReason()).isEqualTo("insufficient_funds");
		assertThat(failed.getAccountUuid()).isNull();
		assertThat(failed.getCustomerUuid()).isNull();
	}

	private CashOperation saveOperation() {
		CashOperation operation = new CashOperation(UUID.randomUUID(), CashOperationType.WITHDRAW, 500L);
		cashOperationRepository.save(operation);

		return operation;
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
