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
import ru.yandex.practicum.mybank.cash.service.AccountsServiceUnavailableException;
import ru.yandex.practicum.mybank.cash.service.CashService;
import ru.yandex.practicum.mybank.chassis.client.ServiceCallException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(PostgresContainerConfig.class)
class CashOrchestrationIntegrationTest {

	private static final UUID ACCOUNT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID CUSTOMER_UUID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");

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

		cashService.deposit("user1", 500);

		Map<String, Object> operation = jdbcTemplate.queryForMap("select * from cash_operations");
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
		assertThat(captor.getValue().transactionUuid()).isEqualTo(operation.get("uuid"));
	}

	@Test
	void rejectedWithdrawalFailsJournalWithoutEvent() {
		when(accountsClient.withdraw(any()))
				.thenThrow(new TransactionRejectedException("insufficient_funds", "Not enough money"));

		assertThatThrownBy(() -> cashService.withdraw("user1", 500))
				.isInstanceOf(TransactionRejectedException.class);

		Map<String, Object> operation = jdbcTemplate.queryForMap("select * from cash_operations");
		assertThat(operation.get("status")).isEqualTo("FAILED");
		assertThat(operation.get("failure_reason")).isEqualTo("insufficient_funds");
		assertThat(operation.get("account_uuid")).isNull();

		Long events = jdbcTemplate.queryForObject("select count(*) from notifications_outbox", Long.class);
		assertThat(events).isZero();
	}

	@Test
	void unavailableAccountsFailsJournal() {
		when(accountsClient.deposit(any())).thenThrow(new ServiceCallException("Call to accounts-service failed"));

		assertThatThrownBy(() -> cashService.deposit("user1", 500))
				.isInstanceOf(AccountsServiceUnavailableException.class);

		Map<String, Object> operation = jdbcTemplate.queryForMap("select * from cash_operations");
		assertThat(operation.get("status")).isEqualTo("FAILED");
		assertThat(operation.get("failure_reason")).isEqualTo("accounts_unavailable");

		Long events = jdbcTemplate.queryForObject("select count(*) from notifications_outbox", Long.class);
		assertThat(events).isZero();
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
