package ru.yandex.practicum.mybank.cash.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mybank.cash.client.TransactionRejectedException;
import ru.yandex.practicum.mybank.cash.domain.CashOperationStatus;
import ru.yandex.practicum.mybank.cash.domain.CashOperationType;
import ru.yandex.practicum.mybank.cash.service.AccountsServiceUnavailableException;
import ru.yandex.practicum.mybank.cash.service.CashService;
import ru.yandex.practicum.mybank.cash.service.DuplicateRequestException;
import ru.yandex.practicum.mybank.cash.service.dto.CashOperationDto;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CashController.class)
@Import(WebSliceConfig.class)
class CashControllerTest {

	private static final UUID OPERATION_UUID = UUID.fromString("7c9e2b40-5a13-4f8e-9d26-1b0a8c4e0001");
	private static final UUID IDEMPOTENCY_KEY = UUID.fromString("7c9e2b40-5a13-4f8e-9d26-1b0a8c4e0002");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CashService cashService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void rejectsAnonymousCaller() throws Exception {
		mockMvc.perform(post("/api/cash/deposit").contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", IDEMPOTENCY_KEY)
						.content("""
								{"amount": 500}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsTokenWithoutWriteScope() throws Exception {
		mockMvc.perform(post("/api/cash/deposit").with(user("user1", "customer:read"))
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", IDEMPOTENCY_KEY)
						.content("""
								{"amount": 500}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void depositsForLoginFromToken() throws Exception {
		when(cashService.deposit(IDEMPOTENCY_KEY, "user1", 500)).thenReturn(new CashOperationDto(
				OPERATION_UUID, CashOperationType.DEPOSIT, 500, CashOperationStatus.COMPLETED));

		mockMvc.perform(post("/api/cash/deposit").with(user("user1", "cash:write"))
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", IDEMPOTENCY_KEY)
						.content("""
								{"amount": 500}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uuid").value(OPERATION_UUID.toString()))
				.andExpect(jsonPath("$.type").value("deposit"))
				.andExpect(jsonPath("$.status").value("completed"));
	}

	@Test
	void rejectsRequestWithoutIdempotencyKey() throws Exception {
		mockMvc.perform(post("/api/cash/deposit").with(user("user1", "cash:write"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"amount": 500}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsMalformedIdempotencyKey() throws Exception {
		mockMvc.perform(post("/api/cash/deposit").with(user("user1", "cash:write"))
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", "not-a-uuid")
						.content("""
								{"amount": 500}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsNonPositiveAmount() throws Exception {
		mockMvc.perform(post("/api/cash/withdraw").with(user("user1", "cash:write"))
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", IDEMPOTENCY_KEY)
						.content("""
								{"amount": 0}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("validation_error"))
				.andExpect(jsonPath("$.validationErrors.fields[0].field").value("amount"));
	}

	@Test
	void translatesRejectionFromAccounts() throws Exception {
		when(cashService.withdraw(IDEMPOTENCY_KEY, "user1", 500))
				.thenThrow(new TransactionRejectedException("insufficient_funds", "Not enough money"));

		mockMvc.perform(post("/api/cash/withdraw").with(user("user1", "cash:write"))
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", IDEMPOTENCY_KEY)
						.content("""
								{"amount": 500}
								"""))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("insufficient_funds"));
	}

	@Test
	void reportsDuplicateRequest() throws Exception {
		when(cashService.deposit(IDEMPOTENCY_KEY, "user1", 500))
				.thenThrow(new DuplicateRequestException(IDEMPOTENCY_KEY));

		mockMvc.perform(post("/api/cash/deposit").with(user("user1", "cash:write"))
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", IDEMPOTENCY_KEY)
						.content("""
								{"amount": 500}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("duplicate_request"));
	}

	@Test
	void reportsAccountsUnavailability() throws Exception {
		when(cashService.deposit(IDEMPOTENCY_KEY, "user1", 500))
				.thenThrow(new AccountsServiceUnavailableException(new RuntimeException("boom")));

		mockMvc.perform(post("/api/cash/deposit").with(user("user1", "cash:write"))
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", IDEMPOTENCY_KEY)
						.content("""
								{"amount": 500}
								"""))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("accounts_service_unavailable"));
	}

	@Test
	void reportsUnexpectedFailureInTheCommonFormat() throws Exception {
		when(cashService.deposit(IDEMPOTENCY_KEY, "user1", 500)).thenThrow(new IllegalStateException("boom"));

		mockMvc.perform(post("/api/cash/deposit").with(user("user1", "cash:write"))
						.contentType(MediaType.APPLICATION_JSON)
						.header("Idempotency-Key", IDEMPOTENCY_KEY)
						.content("""
								{"amount": 500}
								"""))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("internal_error"));
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor user(String login, String scope) {
		return jwt().jwt(jwt -> jwt.claim("preferred_username", login).claim("scope", scope));
	}
}
