package ru.yandex.practicum.mybank.accounts.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mybank.accounts.domain.OperationDirection;
import ru.yandex.practicum.mybank.accounts.domain.TransactionType;
import ru.yandex.practicum.mybank.accounts.service.TransactionsService;
import ru.yandex.practicum.mybank.accounts.service.dto.OperationDto;
import ru.yandex.practicum.mybank.accounts.service.dto.TransactionDto;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import(WebSliceConfig.class)
class TransactionControllerTest {

	private static final UUID TRANSACTION_UUID = UUID.fromString("cccc0001-2222-4333-8444-555566660001");
	private static final UUID ACCOUNT_UUID = UUID.fromString("aaaa0001-2222-4333-8444-555566660001");
	private static final UUID CUSTOMER_UUID = UUID.fromString("3f2a77c4-1e08-4a6b-8f21-9c0d5b7e1111");
	private static final String DEPOSIT_BODY = """
			{"transactionUuid": "cccc0001-2222-4333-8444-555566660001", "login": "user1", "amount": 5000}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TransactionsService transactionsService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void rejectsAnonymousCaller() throws Exception {
		mockMvc.perform(post("/api/transactions/deposit")
						.contentType(MediaType.APPLICATION_JSON)
						.content(DEPOSIT_BODY))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsTokenWithoutTransactionsScope() throws Exception {
		mockMvc.perform(post("/api/transactions/deposit")
						.with(jwt().jwt(jwt -> jwt.claim("scope", "customer:read")))
						.contentType(MediaType.APPLICATION_JSON)
						.content(DEPOSIT_BODY))
				.andExpect(status().isForbidden());
	}

	@Test
	void acceptsTokenWithTransactionsScope() throws Exception {
		when(transactionsService.deposit(any(), anyString(), anyLong())).thenReturn(deposit());

		mockMvc.perform(post("/api/transactions/deposit")
						.with(jwt().jwt(jwt -> jwt.claim("scope", "transactions:write")))
						.contentType(MediaType.APPLICATION_JSON)
						.content(DEPOSIT_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uuid").value(TRANSACTION_UUID.toString()))
				.andExpect(jsonPath("$.type").value("deposit"))
				.andExpect(jsonPath("$.operations.length()").value(1))
				.andExpect(jsonPath("$.operations[0].direction").value("deposit"))
				.andExpect(jsonPath("$.operations[0].toNumber").value("40817810000000000001"))
				.andExpect(jsonPath("$.operations[0].balanceAfter").value(105000))
				.andExpect(jsonPath("$.operations[0].toAccountUuid").value(ACCOUNT_UUID.toString()))
				.andExpect(jsonPath("$.operations[0].toCustomerUuid").value(CUSTOMER_UUID.toString()))
				.andExpect(jsonPath("$.operations[0].fromAccountUuid").doesNotExist());
	}

	@Test
	void reportsValidationErrorsByField() throws Exception {
		mockMvc.perform(post("/api/transactions/deposit")
						.with(jwt().jwt(jwt -> jwt.claim("scope", "transactions:write")))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"login": "", "amount": 0}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("validation_error"))
				.andExpect(jsonPath("$.validationErrors.fields[*].field")
						.value(org.hamcrest.Matchers.hasItems("transactionUuid", "login", "amount")));
	}

	private TransactionDto deposit() {
		return new TransactionDto(TRANSACTION_UUID, TransactionType.DEPOSIT,
				List.of(new OperationDto(OperationDirection.DEPOSIT, null, null, null,
						"40817810000000000001", ACCOUNT_UUID, CUSTOMER_UUID,
						5000, 105000)));
	}
}
