package ru.yandex.practicum.mybank.transfer.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ru.yandex.practicum.mybank.transfer.client.TransactionRejectedException;
import ru.yandex.practicum.mybank.transfer.domain.TransferOperationStatus;
import ru.yandex.practicum.mybank.transfer.service.AccountsServiceUnavailableException;
import ru.yandex.practicum.mybank.transfer.service.DuplicateRequestException;
import ru.yandex.practicum.mybank.transfer.service.TransferService;
import ru.yandex.practicum.mybank.transfer.service.dto.TransferOperationDto;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
@Import(WebSliceConfig.class)
class TransferControllerTest {

	private static final UUID OPERATION_UUID = UUID.fromString("7c9e2b40-5a13-4f8e-9d26-1b0a8c4e0002");
	private static final UUID IDEMPOTENCY_KEY = UUID.fromString("7c9e2b40-5a13-4f8e-9d26-1b0a8c4e0003");
	private static final String BODY = """
			{"toLogin": "user2", "amount": 500}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TransferService transferService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void rejectsAnonymousCaller() throws Exception {
		mockMvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", IDEMPOTENCY_KEY)
						.content(BODY))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsTokenWithoutWriteScope() throws Exception {
		mockMvc.perform(post("/api/transfers").with(user("user1", "cash:write"))
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", IDEMPOTENCY_KEY)
						.content(BODY))
				.andExpect(status().isForbidden());
	}

	@Test
	void transfersFromLoginInToken() throws Exception {
		when(transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500))
				.thenReturn(new TransferOperationDto(OPERATION_UUID, 500, TransferOperationStatus.COMPLETED));

		mockMvc.perform(post("/api/transfers").with(user("user1", "transfer:write"))
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", IDEMPOTENCY_KEY)
						.content(BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uuid").value(OPERATION_UUID.toString()))
				.andExpect(jsonPath("$.amount").value(500))
				.andExpect(jsonPath("$.status").value("completed"));
	}

	@Test
	void rejectsRequestWithoutIdempotencyKey() throws Exception {
		mockMvc.perform(post("/api/transfers").with(user("user1", "transfer:write"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(BODY))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsMalformedIdempotencyKey() throws Exception {
		mockMvc.perform(post("/api/transfers").with(user("user1", "transfer:write"))
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", "not-a-uuid")
						.content(BODY))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsNonPositiveAmount() throws Exception {
		mockMvc.perform(post("/api/transfers").with(user("user1", "transfer:write"))
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", IDEMPOTENCY_KEY)
						.content("""
								{"toLogin": "user2", "amount": 0}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("validation_error"))
				.andExpect(jsonPath("$.validationErrors.fields[0].field").value("amount"));
	}

	@Test
	void rejectsBlankRecipient() throws Exception {
		mockMvc.perform(post("/api/transfers").with(user("user1", "transfer:write"))
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", IDEMPOTENCY_KEY)
						.content("""
								{"toLogin": " ", "amount": 500}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("validation_error"))
				.andExpect(jsonPath("$.validationErrors.fields[0].field").value("toLogin"));
	}

	@Test
	void translatesRejectionFromAccounts() throws Exception {
		when(transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500))
				.thenThrow(new TransactionRejectedException("insufficient_funds", "Not enough money"));

		mockMvc.perform(post("/api/transfers").with(user("user1", "transfer:write"))
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", IDEMPOTENCY_KEY)
						.content(BODY))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("insufficient_funds"));
	}

	@Test
	void reportsDuplicateRequest() throws Exception {
		when(transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500))
				.thenThrow(new DuplicateRequestException(IDEMPOTENCY_KEY));

		mockMvc.perform(post("/api/transfers").with(user("user1", "transfer:write"))
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", IDEMPOTENCY_KEY)
						.content(BODY))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("duplicate_request"));
	}

	@Test
	void hidesTransactionConflictBehindInternalError() throws Exception {
		when(transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500))
				.thenThrow(new TransactionRejectedException("transaction_conflict", "Already applied"));

		mockMvc.perform(post("/api/transfers").with(user("user1", "transfer:write"))
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", IDEMPOTENCY_KEY)
						.content(BODY))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("internal_error"));
	}

	@Test
	void reportsAccountsUnavailability() throws Exception {
		when(transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500))
				.thenThrow(new AccountsServiceUnavailableException(new RuntimeException("boom")));

		mockMvc.perform(post("/api/transfers").with(user("user1", "transfer:write"))
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", IDEMPOTENCY_KEY)
						.content(BODY))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("accounts_unavailable"));
	}

	private static RequestPostProcessor user(String login, String scope) {
		return jwt().jwt(jwt -> jwt.claim("preferred_username", login).claim("scope", scope));
	}
}
