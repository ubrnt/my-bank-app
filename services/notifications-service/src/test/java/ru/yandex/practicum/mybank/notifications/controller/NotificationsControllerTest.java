package ru.yandex.practicum.mybank.notifications.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mybank.notifications.client.CustomerResolutionException;
import ru.yandex.practicum.mybank.notifications.domain.EventType;
import ru.yandex.practicum.mybank.notifications.service.InvalidEventException;
import ru.yandex.practicum.mybank.notifications.service.NotificationsService;
import tools.jackson.databind.JsonNode;

import java.net.ConnectException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationsController.class)
@Import(WebSliceConfig.class)
class NotificationsControllerTest {

	private static final String EVENT_UUID = "1b7f4a90-0d51-4c2e-9f77-0a1e5c3b0001";
	private static final String RECIPIENT_UUID = "3f2a77c4-1e08-4a6b-8f21-9c0d5b7e1111";
	private static final String BODY = """
			{"eventUuid": "%s", "type": "MONEY_DEPOSITED", "recipientUuid": "%s",
			 "payload": {"uuid": "cccc0001-2222-4333-8444-555566660001",
			             "operation": {"toNumber": "40817810000000000001", "amount": 5000, "balanceAfter": 105000}}}
			""".formatted(EVENT_UUID, RECIPIENT_UUID);

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NotificationsService notificationsService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void rejectsAnonymousCaller() throws Exception {
		mockMvc.perform(post("/api/notifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content(BODY))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsTokenWithoutNotificationsScope() throws Exception {
		mockMvc.perform(post("/api/notifications")
						.with(jwt().jwt(jwt -> jwt.claim("scope", "customer:read")))
						.contentType(MediaType.APPLICATION_JSON)
						.content(BODY))
				.andExpect(status().isForbidden());
	}

	@Test
	void passesEventToServiceWithPayloadTree() throws Exception {
		mockMvc.perform(post("/api/notifications")
						.with(serviceToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(BODY))
				.andExpect(status().isOk());

		ArgumentCaptor<JsonNode> payloadCaptor = ArgumentCaptor.forClass(JsonNode.class);
		verify(notificationsService).receive(eq(UUID.fromString(EVENT_UUID)), eq(EventType.MONEY_DEPOSITED),
				eq(UUID.fromString(RECIPIENT_UUID)), payloadCaptor.capture());

		JsonNode payload = payloadCaptor.getValue();
		assertThat(payload.get("operation").get("amount").asLong()).isEqualTo(5000);
	}

	@Test
	void reportsMissingFields() throws Exception {
		mockMvc.perform(post("/api/notifications")
						.with(serviceToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"type": "MONEY_DEPOSITED"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("validation_error"))
				.andExpect(jsonPath("$.validationErrors.fields[*].field")
						.value(org.hamcrest.Matchers.hasItems("eventUuid", "recipientUuid", "payload")));
	}

	@Test
	void reportsInvalidEvent() throws Exception {
		doThrow(new InvalidEventException("Event payload lacks field 'operation.amount'"))
				.when(notificationsService).receive(any(), any(), any(), any());

		mockMvc.perform(post("/api/notifications")
						.with(serviceToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(BODY))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("invalid_event"));
	}

	@Test
	void reportsRecipientResolutionFailure() throws Exception {
		doThrow(new CustomerResolutionException(UUID.fromString(RECIPIENT_UUID), new ConnectException("refused")))
				.when(notificationsService).receive(any(), any(), any(), any());

		mockMvc.perform(post("/api/notifications")
						.with(serviceToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(BODY))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("recipient_resolution_failed"));
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor serviceToken() {
		return jwt().jwt(jwt -> jwt.claim("scope", "notifications:write"));
	}
}
