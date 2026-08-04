package ru.yandex.practicum.mybank.accounts;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionsApiIntegrationTest extends AbstractIntegrationTest {

	private static final String TRANSACTION_UUID = "cccc0001-2222-4333-8444-555566660001";

	@Test
	void depositsMoneyAndWritesEventForOwner() throws Exception {
		mockMvc.perform(post("/api/transactions/deposit")
						.with(serviceToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(deposit(TRANSACTION_UUID, 5000)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uuid").value(TRANSACTION_UUID))
				.andExpect(jsonPath("$.operation.balanceAfter").value(INITIAL_BALANCE + 5000));

		assertThat(balanceOf("user1")).isEqualTo(INITIAL_BALANCE + 5000);
		assertThat(countOf("transactions")).isEqualTo(1);
		assertThat(countOf("balance_operations")).isEqualTo(1);

		assertThat(events()).singleElement().satisfies(event -> {
			assertThat(event.get("event_type")).isEqualTo("MONEY_DEPOSITED");
			assertThat(event.get("status")).isEqualTo("PENDING");
			assertThat(event.get("recipient_login")).isEqualTo("user1");
		});
	}

	@Test
	void repeatedCallMovesMoneyOnceAndAddsNoEvents() throws Exception {
		deposit5000();
		deposit5000();

		assertThat(balanceOf("user1")).isEqualTo(INITIAL_BALANCE + 5000);
		assertThat(countOf("transactions")).isEqualTo(1);
		assertThat(countOf("balance_operations")).isEqualTo(1);
		assertThat(events()).hasSize(1);
	}

	@Test
	void rejectsSameTransactionWithOtherDetails() throws Exception {
		deposit5000();

		mockMvc.perform(post("/api/transactions/deposit")
						.with(serviceToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(deposit(TRANSACTION_UUID, 6000)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("transaction_conflict"));

		assertThat(balanceOf("user1")).isEqualTo(INITIAL_BALANCE + 5000);
	}

	@Test
	void leavesNoEventWhenFundsAreInsufficient() throws Exception {
		mockMvc.perform(post("/api/transactions/withdraw")
						.with(serviceToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"transactionUuid": "%s", "login": "user1", "amount": %d}
								""".formatted(TRANSACTION_UUID, INITIAL_BALANCE + 1)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("insufficient_funds"));

		assertThat(balanceOf("user1")).isEqualTo(INITIAL_BALANCE);
		assertThat(events()).isEmpty();
	}

	@Test
	void writesEventForEachSideOfTransfer() throws Exception {
		mockMvc.perform(post("/api/transactions/transfer")
						.with(serviceToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"transactionUuid": "%s", "fromLogin": "user1", "toLogin": "user2", "amount": 3000}
								""".formatted(TRANSACTION_UUID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.operation.direction").value("withdraw"));

		assertThat(balanceOf("user1")).isEqualTo(INITIAL_BALANCE - 3000);
		assertThat(balanceOf("user2")).isEqualTo(INITIAL_BALANCE + 3000);
		assertThat(countOf("balance_operations")).isEqualTo(2);

		assertThat(events())
				.extracting(event -> event.get("event_type") + " -> " + event.get("recipient_login"))
				.containsExactlyInAnyOrder("MONEY_SENT -> user1", "MONEY_RECEIVED -> user2");
	}

	private void deposit5000() throws Exception {
		mockMvc.perform(post("/api/transactions/deposit")
						.with(serviceToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(deposit(TRANSACTION_UUID, 5000)))
				.andExpect(status().isOk());
	}

	private String deposit(String transactionUuid, long amount) {
		return """
				{"transactionUuid": "%s", "login": "user1", "amount": %d}
				""".formatted(transactionUuid, amount);
	}

	private List<Map<String, Object>> events() {
		return jdbcTemplate.queryForList("""
				select event_type, status, attempts, recipient ->> 'login' as recipient_login
				  from outbox_events
				 order by id
				""");
	}
}
