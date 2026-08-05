package ru.yandex.practicum.mybank.accounts;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionsApiIntegrationTest extends AbstractIntegrationTest {

	private static final String TRANSACTION_UUID = "cccc0001-2222-4333-8444-555566660001";

	@Test
	void depositsMoneyWithoutNotificationEvents() throws Exception {
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
		assertThat(countOf("outbox_events")).isZero();
	}

	@Test
	void repeatedCallMovesMoneyOnce() throws Exception {
		deposit5000();
		deposit5000();

		assertThat(balanceOf("user1")).isEqualTo(INITIAL_BALANCE + 5000);
		assertThat(countOf("transactions")).isEqualTo(1);
		assertThat(countOf("balance_operations")).isEqualTo(1);
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
	void rejectsWithdrawalWhenFundsAreInsufficient() throws Exception {
		mockMvc.perform(post("/api/transactions/withdraw")
						.with(serviceToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"transactionUuid": "%s", "login": "user1", "amount": %d}
								""".formatted(TRANSACTION_UUID, INITIAL_BALANCE + 1)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("insufficient_funds"));

		assertThat(balanceOf("user1")).isEqualTo(INITIAL_BALANCE);
		assertThat(countOf("transactions")).isZero();
		assertThat(countOf("balance_operations")).isZero();
	}

	@Test
	void transfersMoneyBetweenAccounts() throws Exception {
		mockMvc.perform(post("/api/transactions/transfer")
						.with(serviceToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"transactionUuid": "%s", "fromLogin": "user1", "toLogin": "user2", "amount": 3000}
								""".formatted(TRANSACTION_UUID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.operation.direction").value("withdraw"))
				.andExpect(jsonPath("$.operation.fromAccountUuid").isNotEmpty())
				.andExpect(jsonPath("$.operation.fromCustomerUuid").isNotEmpty())
				.andExpect(jsonPath("$.operation.toAccountUuid").isNotEmpty())
				.andExpect(jsonPath("$.operation.toCustomerUuid").isNotEmpty());

		assertThat(balanceOf("user1")).isEqualTo(INITIAL_BALANCE - 3000);
		assertThat(balanceOf("user2")).isEqualTo(INITIAL_BALANCE + 3000);
		assertThat(countOf("balance_operations")).isEqualTo(2);
		assertThat(countOf("outbox_events")).isZero();
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
}
