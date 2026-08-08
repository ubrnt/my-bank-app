package ru.yandex.practicum.mybank.cash;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.mybank.cash.client.AccountsClient;
import ru.yandex.practicum.mybank.cash.client.TransactionRejectedException;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionOperation;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionRequest;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionResponse;
import ru.yandex.practicum.mybank.cash.domain.CashOperationStatus;
import ru.yandex.practicum.mybank.cash.service.AccountsServiceUnavailableException;
import ru.yandex.practicum.mybank.cash.service.CashService;
import ru.yandex.practicum.mybank.cash.service.DuplicateRequestException;
import ru.yandex.practicum.mybank.cash.service.dto.CashOperationDto;
import ru.yandex.practicum.mybank.chassis.client.ServiceCallException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(PostgresContainerConfig.class)
class CashOrchestrationIntegrationTest {

	private static final UUID ACCOUNT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID CUSTOMER_UUID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
	private static final UUID IDEMPOTENCY_KEY = UUID.fromString("bbbbbbbb-1111-1111-1111-111111111111");

	@Autowired
	private CashService cashService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private AccountsClient accountsClient;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@BeforeEach
	void resetData() {
		jdbcTemplate.execute("truncate table cash_operations, notifications_outbox");
	}

	@Test
	void depositCompletesJournalAndEmitsEventInOneTransaction() {
		when(accountsClient.deposit(any())).thenAnswer(invocation -> response(
				invocation.getArgument(0, TransactionRequest.class), "deposit"));

		cashService.deposit(IDEMPOTENCY_KEY, "user1", 500);

		Map<String, Object> operation = jdbcTemplate.queryForMap("select * from cash_operations");
		assertThat(operation.get("uuid")).isEqualTo(IDEMPOTENCY_KEY);
		assertThat(operation.get("status")).isEqualTo("COMPLETED");
		assertThat(operation.get("account_uuid")).isEqualTo(ACCOUNT_UUID);
		assertThat(operation.get("customer_uuid")).isEqualTo(CUSTOMER_UUID);
		assertThat(operation.get("failure_reason")).isNull();

		Map<String, Object> event = jdbcTemplate.queryForMap("select * from notifications_outbox");
		assertThat(event.get("event_type")).isEqualTo("MONEY_DEPOSITED");
		assertThat(event.get("recipient_uuid")).isEqualTo(CUSTOMER_UUID);
		assertThat(event.get("status")).isEqualTo("PENDING");

		ArgumentCaptor<TransactionRequest> captor = ArgumentCaptor.forClass(TransactionRequest.class);
		verify(accountsClient).deposit(captor.capture());
		assertThat(captor.getValue().transactionUuid()).isEqualTo(IDEMPOTENCY_KEY);
	}

	@Test
	void rejectedWithdrawalFailsJournalWithoutEvent() {
		when(accountsClient.withdraw(any()))
				.thenThrow(new TransactionRejectedException("insufficient_funds", "Not enough money"));

		assertThatThrownBy(() -> cashService.withdraw(IDEMPOTENCY_KEY, "user1", 500))
				.isInstanceOf(TransactionRejectedException.class);

		Map<String, Object> operation = jdbcTemplate.queryForMap("select * from cash_operations");
		assertThat(operation.get("status")).isEqualTo("FAILED");
		assertThat(operation.get("failure_reason")).isEqualTo("insufficient_funds");
		assertThat(operation.get("customer_login")).isEqualTo("user1");
		assertThat(operation.get("account_uuid")).isNull();

		Long events = jdbcTemplate.queryForObject("select count(*) from notifications_outbox", Long.class);
		assertThat(events).isZero();
	}

	@Test
	void unavailableAccountsFailsJournal() {
		when(accountsClient.deposit(any())).thenThrow(new ServiceCallException("Call to accounts-service failed"));

		assertThatThrownBy(() -> cashService.deposit(IDEMPOTENCY_KEY, "user1", 500))
				.isInstanceOf(AccountsServiceUnavailableException.class);

		Map<String, Object> operation = jdbcTemplate.queryForMap("select * from cash_operations");
		assertThat(operation.get("status")).isEqualTo("FAILED");
		assertThat(operation.get("failure_reason")).isEqualTo("accounts_unavailable");

		Long events = jdbcTemplate.queryForObject("select count(*) from notifications_outbox", Long.class);
		assertThat(events).isZero();
	}

	@Test
	void repeatOfCompletedOperationSkipsAccounts() {
		when(accountsClient.deposit(any())).thenAnswer(invocation -> response(
				invocation.getArgument(0, TransactionRequest.class), "deposit"));

		cashService.deposit(IDEMPOTENCY_KEY, "user1", 500);
		CashOperationDto repeated = cashService.deposit(IDEMPOTENCY_KEY, "user1", 500);

		assertThat(repeated.uuid()).isEqualTo(IDEMPOTENCY_KEY);
		assertThat(repeated.status()).isEqualTo(CashOperationStatus.COMPLETED);

		verify(accountsClient, times(1)).deposit(any());
		assertThat(operationCount()).isOne();

		Long events = jdbcTemplate.queryForObject("select count(*) from notifications_outbox", Long.class);
		assertThat(events).isOne();
	}

	@Test
	void repeatOfFailedOperationCallsAccountsAgain() {
		when(accountsClient.withdraw(any()))
				.thenThrow(new TransactionRejectedException("insufficient_funds", "Not enough money"))
				.thenAnswer(invocation -> response(invocation.getArgument(0, TransactionRequest.class), "withdraw"));

		assertThatThrownBy(() -> cashService.withdraw(IDEMPOTENCY_KEY, "user1", 500))
				.isInstanceOf(TransactionRejectedException.class);

		CashOperationDto repeated = cashService.withdraw(IDEMPOTENCY_KEY, "user1", 500);

		assertThat(repeated.status()).isEqualTo(CashOperationStatus.COMPLETED);

		Map<String, Object> operation = jdbcTemplate.queryForMap("select * from cash_operations");
		assertThat(operation.get("status")).isEqualTo("COMPLETED");
		assertThat(operation.get("failure_reason")).isNull();

		verify(accountsClient, times(2)).withdraw(any());
		assertThat(operationCount()).isOne();
	}

	@Test
	void repeatWhileFirstRequestIsInFlightIsRejected() {
		insertPendingOperation(IDEMPOTENCY_KEY, 0);

		assertThatThrownBy(() -> cashService.deposit(IDEMPOTENCY_KEY, "user1", 500))
				.isInstanceOf(DuplicateRequestException.class);

		verifyNoInteractions(accountsClient);
		assertThat(operationCount()).isOne();
	}

	@Test
	void expiredClaimIsPickedUpByRepeat() {
		insertPendingOperation(IDEMPOTENCY_KEY, 3600);
		when(accountsClient.deposit(any())).thenAnswer(invocation -> response(
				invocation.getArgument(0, TransactionRequest.class), "deposit"));

		CashOperationDto repeated = cashService.deposit(IDEMPOTENCY_KEY, "user1", 500);

		assertThat(repeated.status()).isEqualTo(CashOperationStatus.COMPLETED);

		Map<String, Object> operation = jdbcTemplate.queryForMap("select * from cash_operations");
		assertThat(operation.get("status")).isEqualTo("COMPLETED");

		verify(accountsClient).deposit(any());
		assertThat(operationCount()).isOne();
	}

	private void insertPendingOperation(UUID uuid, int ageSeconds) {
		jdbcTemplate.update("""
				insert into cash_operations (uuid, customer_login, type, amount, status, created_ts, updated_ts, version)
				values (?, 'user1', 'DEPOSIT', 500, 'PENDING', now(), now() - (? * interval '1 second'), 0)
				""", uuid, ageSeconds);
	}

	private long operationCount() {
		return jdbcTemplate.queryForObject("select count(*) from cash_operations", Long.class);
	}

	private TransactionResponse response(TransactionRequest request, String type) {
		TransactionOperation operation = switch (type) {
			case "deposit" -> new TransactionOperation("deposit", "40817810000000000001",
					ACCOUNT_UUID, CUSTOMER_UUID, null, null, null, request.amount(), 25500L);
			default -> new TransactionOperation("withdraw", null, null, null,
					"40817810000000000001", ACCOUNT_UUID, CUSTOMER_UUID, request.amount(), 24500L);
		};

		return new TransactionResponse(request.transactionUuid(), type, List.of(operation));
	}
}
