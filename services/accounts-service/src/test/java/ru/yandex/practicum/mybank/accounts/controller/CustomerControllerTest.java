package ru.yandex.practicum.mybank.accounts.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.yandex.practicum.mybank.accounts.service.CustomerService;
import ru.yandex.practicum.mybank.accounts.service.dto.CustomerAccountDto;
import ru.yandex.practicum.mybank.accounts.service.dto.CustomerDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import(WebSliceConfig.class)
class CustomerControllerTest {

	private static final UUID CUSTOMER_UUID = UUID.fromString("3f2a77c4-1e08-4a6b-8f21-9c0d5b7e1111");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CustomerService customerService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void rejectsAnonymousCaller() throws Exception {
		mockMvc.perform(get("/api/customers/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsTokenWithoutReadScope() throws Exception {
		mockMvc.perform(get("/api/customers/me").with(user("user1", "customer:write")))
				.andExpect(status().isForbidden());
	}

	@Test
	void returnsOwnAccountByLoginFromToken() throws Exception {
		when(customerService.getCustomerAccount("user1")).thenReturn(new CustomerAccountDto(
				"user1", "user1_first_name user1_last_name", LocalDate.of(1990, 1, 15), "40817810000000000001", 100000));

		mockMvc.perform(get("/api/customers/me").with(user("user1", "customer:read")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.login").value("user1"))
				.andExpect(jsonPath("$.number").value("40817810000000000001"))
				.andExpect(jsonPath("$.balance").value(100000));
	}

	@Test
	void returnsOthersWithoutAccounts() throws Exception {
		when(customerService.findOthers(anyString()))
				.thenReturn(List.of(new CustomerDto(CUSTOMER_UUID, "user2", "user2_first_name user2_last_name")));

		mockMvc.perform(get("/api/customers/others").with(user("user1", "customer:others:read")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].login").value("user2"))
				.andExpect(jsonPath("$[0].name").value("user2_first_name user2_last_name"))
				.andExpect(jsonPath("$[0].number").doesNotExist())
				.andExpect(jsonPath("$[0].balance").doesNotExist());
	}

	@Test
	void rejectsProfileOfMinor() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/api/customers/me")
						.with(user("user1", "customer:write"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "user1_first_name user1_last_name", "birthdate": "2020-01-15"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("validation_error"))
				.andExpect(jsonPath("$.validationErrors.fields[0].field").value("birthdate"));
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor user(String login, String scope) {
		return jwt().jwt(jwt -> jwt.claim("preferred_username", login).claim("scope", scope));
	}
}
