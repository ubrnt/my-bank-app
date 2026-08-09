package ru.yandex.practicum.mybank.transfer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.mybank.chassis.client.ServiceCallException;
import ru.yandex.practicum.mybank.transfer.client.AccountsClient;
import ru.yandex.practicum.mybank.transfer.client.TransactionRejectedException;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionOperation;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionRequest;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionResponse;
import ru.yandex.practicum.mybank.transfer.domain.TransferOperationStatus;
import ru.yandex.practicum.mybank.transfer.service.AccountsServiceUnavailableException;
import ru.yandex.practicum.mybank.transfer.service.DuplicateRequestException;
import ru.yandex.practicum.mybank.transfer.service.IdempotencyKeyConflictException;
import ru.yandex.practicum.mybank.transfer.service.TransferService;
import ru.yandex.practicum.mybank.transfer.service.dto.TransferOperationDto;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

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
class TransferOrchestrationIntegrationTest {

	private static final String FROM_NUMBER = "40817810000000000001";
	private static final UUID FROM_ACCOUNT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID FROM_CUSTOMER_UUID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
	private static final String TO_NUMBER = "40817810000000000002";
	private static final UUID TO_ACCOUNT_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID TO_CUSTOMER_UUID = UUID.fromString("aaaaaaaa-2222-2222-2222-222222222222");
	private static final UUID IDEMPOTENCY_KEY = UUID.fromString("bbbbbbbb-1111-1111-1111-111111111111");

	@Autowired
	private TransferService transferService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private AccountsClient accountsClient;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@BeforeEach
	void resetData() {
		jdbcTemplate.execute("truncate table transfer_operations, notifications_outbox");
	}

	@Test
	void completedTransferEmitsAnEventPerSide() {
		when(accountsClient.transfer(any())).thenAnswer(invocation ->
				response(invocation.getArgument(0, TransactionRequest.class)));

		transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500);

		Map<String, Object> operation = jdbcTemplate.queryForMap("select * from transfer_operations");
		assertThat(operation.get("uuid")).isEqualTo(IDEMPOTENCY_KEY);
		assertThat(operation.get("status")).isEqualTo("COMPLETED");
		assertThat(operation.get("from_account_uuid")).isEqualTo(FROM_ACCOUNT_UUID);
		assertThat(operation.get("from_customer_uuid")).isEqualTo(FROM_CUSTOMER_UUID);
		assertThat(operation.get("to_account_uuid")).isEqualTo(TO_ACCOUNT_UUID);
		assertThat(operation.get("to_customer_uuid")).isEqualTo(TO_CUSTOMER_UUID);
		assertThat(operation.get("failure_reason")).isNull();

		List<Map<String, Object>> events = jdbcTemplate.queryForList(
				"select * from notifications_outbox order by event_type");

		assertThat(events).hasSize(2);
		assertThat(events.getFirst().get("event_type")).isEqualTo("MONEY_RECEIVED");
		assertThat(events.getFirst().get("recipient_uuid")).isEqualTo(TO_CUSTOMER_UUID);
		assertThat(events.getLast().get("event_type")).isEqualTo("MONEY_SENT");
		assertThat(events.getLast().get("recipient_uuid")).isEqualTo(FROM_CUSTOMER_UUID);

		JsonNode received = operationOf(events.getFirst());
		assertThat(received.get("direction").asString()).isEqualTo("deposit");
		assertThat(received.get("balanceAfter").asLong()).isEqualTo(5500);

		JsonNode sent = operationOf(events.getLast());
		assertThat(sent.get("direction").asString()).isEqualTo("withdraw");
		assertThat(sent.get("balanceAfter").asLong()).isEqualTo(24500);
	}

	@Test
	void rejectedTransferFailsJournalWithoutEvents() {
		when(accountsClient.transfer(any()))
				.thenThrow(new TransactionRejectedException("insufficient_funds", "Not enough money"));

		assertThatThrownBy(() -> transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500))
				.isInstanceOf(TransactionRejectedException.class);

		assertFailedWithReason("insufficient_funds");
	}

	@Test
	void unknownRecipientFailsJournalWithoutEvents() {
		when(accountsClient.transfer(any()))
				.thenThrow(new TransactionRejectedException("customer_account_not_found", "No such recipient"));

		assertThatThrownBy(() -> transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500))
				.isInstanceOf(TransactionRejectedException.class);

		assertFailedWithReason("customer_account_not_found");
	}

	@Test
	void unavailableAccountsFailsJournalWithoutEvents() {
		when(accountsClient.transfer(any()))
				.thenThrow(new ServiceCallException("Call to accounts-service failed"));

		assertThatThrownBy(() -> transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500))
				.isInstanceOf(AccountsServiceUnavailableException.class);

		assertFailedWithReason("accounts_unavailable");
	}

	@Test
	void repeatOfCompletedTransferSkipsAccounts() {
		when(accountsClient.transfer(any())).thenAnswer(invocation ->
				response(invocation.getArgument(0, TransactionRequest.class)));

		transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500);
		TransferOperationDto repeated = transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500);

		assertThat(repeated.uuid()).isEqualTo(IDEMPOTENCY_KEY);
		assertThat(repeated.status()).isEqualTo(TransferOperationStatus.COMPLETED);

		verify(accountsClient, times(1)).transfer(any());
		assertThat(operationCount()).isOne();

		Long events = jdbcTemplate.queryForObject("select count(*) from notifications_outbox", Long.class);
		assertThat(events).isEqualTo(2);
	}

	@Test
	void repeatOfFailedTransferCallsAccountsAgain() {
		when(accountsClient.transfer(any()))
				.thenThrow(new TransactionRejectedException("insufficient_funds", "Not enough money"))
				.thenAnswer(invocation -> response(invocation.getArgument(0, TransactionRequest.class)));

		assertThatThrownBy(() -> transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500))
				.isInstanceOf(TransactionRejectedException.class);

		TransferOperationDto repeated = transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500);

		assertThat(repeated.status()).isEqualTo(TransferOperationStatus.COMPLETED);

		Map<String, Object> operation = jdbcTemplate.queryForMap("select * from transfer_operations");
		assertThat(operation.get("status")).isEqualTo("COMPLETED");
		assertThat(operation.get("failure_reason")).isNull();

		verify(accountsClient, times(2)).transfer(any());
		assertThat(operationCount()).isOne();
	}

	@Test
	void repeatWithOtherRecipientIsRejected() {
		when(accountsClient.transfer(any())).thenAnswer(invocation ->
				response(invocation.getArgument(0, TransactionRequest.class)));

		transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500);

		assertThatThrownBy(() -> transferService.transfer(IDEMPOTENCY_KEY, "user1", "user3", 500))
				.isInstanceOf(IdempotencyKeyConflictException.class);

		Map<String, Object> operation = jdbcTemplate.queryForMap("select * from transfer_operations");
		assertThat(operation.get("to_customer_login")).isEqualTo("user2");

		verify(accountsClient, times(1)).transfer(any());
		assertThat(operationCount()).isOne();
	}

	@Test
	void repeatWithOtherAmountIsRejected() {
		when(accountsClient.transfer(any()))
				.thenThrow(new TransactionRejectedException("insufficient_funds", "Not enough money"));

		assertThatThrownBy(() -> transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500))
				.isInstanceOf(TransactionRejectedException.class);

		assertThatThrownBy(() -> transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 300))
				.isInstanceOf(IdempotencyKeyConflictException.class);

		Map<String, Object> operation = jdbcTemplate.queryForMap("select * from transfer_operations");
		assertThat(operation.get("status")).isEqualTo("FAILED");
		assertThat(operation.get("amount")).isEqualTo(500L);

		verify(accountsClient).transfer(any());
		assertThat(operationCount()).isOne();
	}

	@Test
	void repeatWhileFirstRequestIsInFlightIsRejected() {
		insertPendingOperation(IDEMPOTENCY_KEY, 0);

		assertThatThrownBy(() -> transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500))
				.isInstanceOf(DuplicateRequestException.class);

		verifyNoInteractions(accountsClient);
		assertThat(operationCount()).isOne();
	}

	@Test
	void expiredClaimIsPickedUpByRepeat() {
		insertPendingOperation(IDEMPOTENCY_KEY, 3600);
		when(accountsClient.transfer(any())).thenAnswer(invocation ->
				response(invocation.getArgument(0, TransactionRequest.class)));

		TransferOperationDto repeated = transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500);

		assertThat(repeated.status()).isEqualTo(TransferOperationStatus.COMPLETED);

		Map<String, Object> operation = jdbcTemplate.queryForMap("select * from transfer_operations");
		assertThat(operation.get("status")).isEqualTo("COMPLETED");

		verify(accountsClient).transfer(any());
		assertThat(operationCount()).isOne();
	}

	private void insertPendingOperation(UUID uuid, int ageSeconds) {
		jdbcTemplate.update("""
				insert into transfer_operations (uuid, from_customer_login, to_customer_login, amount, status, created_ts, updated_ts, version)
				values (?, 'user1', 'user2', 500, 'PENDING', now(), now() - make_interval(secs => ?), 0)
				""", uuid, ageSeconds);
	}

	private long operationCount() {
		return jdbcTemplate.queryForObject("select count(*) from transfer_operations", Long.class);
	}

	private void assertFailedWithReason(String reason) {
		Map<String, Object> operation = jdbcTemplate.queryForMap("select * from transfer_operations");
		assertThat(operation.get("status")).isEqualTo("FAILED");
		assertThat(operation.get("failure_reason")).isEqualTo(reason);
		assertThat(operation.get("from_customer_login")).isEqualTo("user1");
		assertThat(operation.get("from_account_uuid")).isNull();
		assertThat(operation.get("to_account_uuid")).isNull();

		Long events = jdbcTemplate.queryForObject("select count(*) from notifications_outbox", Long.class);
		assertThat(events).isZero();
	}

	private JsonNode operationOf(Map<String, Object> event) {
		return JsonMapper.builder().build()
				.readTree(String.valueOf(event.get("payload")))
				.get("operation");
	}

	private TransactionResponse response(TransactionRequest request) {
		TransactionOperation sent = new TransactionOperation("withdraw",
				TO_NUMBER, TO_ACCOUNT_UUID, TO_CUSTOMER_UUID,
				FROM_NUMBER, FROM_ACCOUNT_UUID, FROM_CUSTOMER_UUID,
				request.amount(), 24500L);
		TransactionOperation received = new TransactionOperation("deposit",
				TO_NUMBER, TO_ACCOUNT_UUID, TO_CUSTOMER_UUID,
				FROM_NUMBER, FROM_ACCOUNT_UUID, FROM_CUSTOMER_UUID,
				request.amount(), 5500L);

		return new TransactionResponse(request.transactionUuid(), "transfer", List.of(sent, received));
	}
}
