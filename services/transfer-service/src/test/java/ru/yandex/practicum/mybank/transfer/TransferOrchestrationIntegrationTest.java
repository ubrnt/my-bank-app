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
import ru.yandex.practicum.mybank.transfer.service.AccountsServiceUnavailableException;
import ru.yandex.practicum.mybank.transfer.service.TransferService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

		transferService.transfer("user1", "user2", 500);

		Map<String, Object> operation = jdbcTemplate.queryForMap("select * from transfer_operations");
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

		assertThatThrownBy(() -> transferService.transfer("user1", "user2", 500))
				.isInstanceOf(TransactionRejectedException.class);

		assertFailedWithReason("insufficient_funds");
	}

	@Test
	void unknownRecipientFailsJournalWithoutEvents() {
		when(accountsClient.transfer(any()))
				.thenThrow(new TransactionRejectedException("customer_account_not_found", "No such recipient"));

		assertThatThrownBy(() -> transferService.transfer("user1", "user2", 500))
				.isInstanceOf(TransactionRejectedException.class);

		assertFailedWithReason("customer_account_not_found");
	}

	@Test
	void unavailableAccountsFailsJournalWithoutEvents() {
		when(accountsClient.transfer(any()))
				.thenThrow(new ServiceCallException("Call to accounts-service failed"));

		assertThatThrownBy(() -> transferService.transfer("user1", "user2", 500))
				.isInstanceOf(AccountsServiceUnavailableException.class);

		assertFailedWithReason("accounts_unavailable");
	}

	private void assertFailedWithReason(String reason) {
		Map<String, Object> operation = jdbcTemplate.queryForMap("select * from transfer_operations");
		assertThat(operation.get("status")).isEqualTo("FAILED");
		assertThat(operation.get("failure_reason")).isEqualTo(reason);
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
