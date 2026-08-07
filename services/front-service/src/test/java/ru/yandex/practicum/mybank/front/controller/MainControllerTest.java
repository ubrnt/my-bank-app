package ru.yandex.practicum.mybank.front.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mybank.front.client.GatewayClient;
import ru.yandex.practicum.mybank.front.client.GatewayException;
import ru.yandex.practicum.mybank.front.client.dto.CashRequest;
import ru.yandex.practicum.mybank.front.client.dto.CustomerResponse;
import ru.yandex.practicum.mybank.front.client.dto.CustomerSummaryResponse;
import ru.yandex.practicum.mybank.front.client.dto.ErrorResponse;
import ru.yandex.practicum.mybank.front.client.dto.FieldError;
import ru.yandex.practicum.mybank.front.client.dto.TransferRequest;
import ru.yandex.practicum.mybank.front.client.dto.UpdateProfileRequest;
import ru.yandex.practicum.mybank.front.client.dto.ValidationErrors;
import ru.yandex.practicum.mybank.front.controller.dto.AccountDto;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(MainController.class)
@Import(WebSliceConfig.class)
class MainControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GatewayClient gatewayClient;

	@BeforeEach
	void customerIsReadable() {
		when(gatewayClient.getCustomer()).thenReturn(new CustomerResponse("user1", "Иванов Иван",
				LocalDate.of(1990, 1, 15), "40817810000000000001", 1000));
		when(gatewayClient.getOtherCustomers())
				.thenReturn(List.of(new CustomerSummaryResponse("user2", "Петров Пётр")));
	}

	@Test
	void redirectsAnonymousToLogin() throws Exception {
		mockMvc.perform(get("/account"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/oauth2/authorization/keycloak"));
	}

	@Test
	void showsCustomerData() throws Exception {
		mockMvc.perform(get("/account").with(oidcLogin()))
				.andExpect(status().isOk())
				.andExpect(view().name("main"))
				.andExpect(model().attribute("name", "Иванов Иван"))
				.andExpect(model().attribute("birthdate", "1990-01-15"))
				.andExpect(model().attribute("sum", 1000L))
				.andExpect(model().attribute("accounts", List.of(new AccountDto("user2", "Петров Пётр"))));
	}

	@Test
	void updatesProfile() throws Exception {
		mockMvc.perform(post("/account").with(oidcLogin()).with(csrf())
						.param("name", "Иванов Иван")
						.param("birthdate", "1990-01-15"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("info", "Данные сохранены"));

		verify(gatewayClient).updateCustomer(new UpdateProfileRequest("Иванов Иван", LocalDate.of(1990, 1, 15)));
	}

	@Test
	void depositsCash() throws Exception {
		mockMvc.perform(post("/cash").with(oidcLogin()).with(csrf())
						.param("value", "100")
						.param("action", "PUT"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("info", "Положено 100 руб"));

		verify(gatewayClient).deposit(new CashRequest(100));
	}

	@Test
	void withdrawsCash() throws Exception {
		mockMvc.perform(post("/cash").with(oidcLogin()).with(csrf())
						.param("value", "100")
						.param("action", "GET"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("info", "Снято 100 руб"));

		verify(gatewayClient).withdraw(new CashRequest(100));
	}

	@Test
	void transfersMoney() throws Exception {
		mockMvc.perform(post("/transfer").with(oidcLogin()).with(csrf())
						.param("value", "300")
						.param("login", "user2"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("info", "Успешно переведено 300 руб клиенту user2"));

		verify(gatewayClient).transfer(new TransferRequest("user2", 300));
	}

	@Test
	void showsRejectionByCode() throws Exception {
		rejectWithdrawal(new ErrorResponse("insufficient_funds", "Not enough money", null));

		mockMvc.perform(post("/cash").with(oidcLogin()).with(csrf())
						.param("value", "100")
						.param("action", "GET"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("errors", List.of("Недостаточно средств на счёте")))
				.andExpect(model().attribute("info", (Object) null));
	}

	@Test
	void showsRejectionByField() throws Exception {
		ValidationErrors fields = new ValidationErrors(List.of(new FieldError("birthdate", "must be an adult")));
		doThrow(new GatewayException(new ErrorResponse("validation_error", "Request validation failed", fields),
				new RuntimeException()))
				.when(gatewayClient).updateCustomer(any());

		mockMvc.perform(post("/account").with(oidcLogin()).with(csrf())
						.param("name", "Иванов Иван")
						.param("birthdate", "2020-01-15"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("errors", List.of("Возраст должен быть не меньше 18 лет")));
	}

	@Test
	void showsGenericTextForUnknownCode() throws Exception {
		rejectWithdrawal(new ErrorResponse("teapot_on_fire", "Whatever", null));

		mockMvc.perform(post("/cash").with(oidcLogin()).with(csrf())
						.param("value", "100")
						.param("action", "GET"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("errors",
						List.of("Не удалось выполнить операцию, попробуйте позже")));
	}

	@Test
	void showsGenericTextWhenGatewayDoesNotAnswer() throws Exception {
		rejectWithdrawal(null);

		mockMvc.perform(post("/cash").with(oidcLogin()).with(csrf())
						.param("value", "100")
						.param("action", "GET"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("errors",
						List.of("Не удалось выполнить операцию, попробуйте позже")));
	}

	@Test
	void showsErrorWhenCustomerCannotBeRead() throws Exception {
		when(gatewayClient.getCustomer())
				.thenThrow(new GatewayException(new ErrorResponse("accounts_unavailable", "Down", null),
						new RuntimeException()));

		mockMvc.perform(get("/account").with(oidcLogin()))
				.andExpect(status().isOk())
				.andExpect(model().attribute("errors", List.of("Сервис счетов временно недоступен")));
	}

	@Test
	void rejectsPostWithoutCsrfToken() throws Exception {
		mockMvc.perform(post("/cash").with(oidcLogin())
						.param("value", "100")
						.param("action", "PUT"))
				.andExpect(status().isForbidden());
	}

	private void rejectWithdrawal(ErrorResponse response) {
		doThrow(new GatewayException(response, new RuntimeException()))
				.when(gatewayClient).withdraw(any());
	}
}
