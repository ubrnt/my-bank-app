package ru.yandex.practicum.mybank.transfer;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.mybank.chassis.client.ServiceCallException;
import ru.yandex.practicum.mybank.transfer.client.AccountsClient;
import ru.yandex.practicum.mybank.transfer.client.TransactionRejectedException;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionRequest;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "mybank.accounts.retry.delay=1ms")
@Import({PostgresContainerConfig.class, FakeTokenConfig.class})
class AccountsClientTest {

	private static final String TRANSFER_URL = "/api/transactions/transfer";

	@RegisterExtension
	static WireMockExtension accounts = WireMockExtension.newInstance()
			.options(options().dynamicPort())
			.build();

	@Autowired
	private AccountsClient accountsClient;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@DynamicPropertySource
	static void discovery(DynamicPropertyRegistry registry) {
		registry.add("spring.cloud.discovery.client.simple.instances.accounts-service[0].uri", accounts::baseUrl);
	}

	@Test
	void insufficientFundsIsRejectedWithoutRetries() {
		accounts.stubFor(post(urlEqualTo(TRANSFER_URL))
				.willReturn(errorResponse(422, "insufficient_funds", "Not enough money")));

		assertThatThrownBy(() -> accountsClient.transfer(request()))
				.isInstanceOf(TransactionRejectedException.class)
				.extracting(e -> ((TransactionRejectedException) e).getCode())
				.isEqualTo("insufficient_funds");

		accounts.verify(1, postRequestedFor(urlEqualTo(TRANSFER_URL)));
	}

	@Test
	void repeatWithDifferentDetailsIsRejectedWithoutRetries() {
		accounts.stubFor(post(urlEqualTo(TRANSFER_URL))
				.willReturn(errorResponse(409, "transaction_conflict", "Transaction already applied")));

		assertThatThrownBy(() -> accountsClient.transfer(request()))
				.isInstanceOf(TransactionRejectedException.class)
				.extracting(e -> ((TransactionRejectedException) e).getCode())
				.isEqualTo("transaction_conflict");

		accounts.verify(1, postRequestedFor(urlEqualTo(TRANSFER_URL)));
	}

	@Test
	void serverErrorIsRetriedUntilAttemptsRunOut() {
		accounts.stubFor(post(urlEqualTo(TRANSFER_URL)).willReturn(aResponse().withStatus(500)));

		assertThatThrownBy(() -> accountsClient.transfer(request()))
				.isInstanceOf(ServiceCallException.class);

		accounts.verify(3, postRequestedFor(urlEqualTo(TRANSFER_URL)));
	}

	private static TransactionRequest request() {
		return new TransactionRequest(UUID.randomUUID(), "user1", "user2", 500);
	}

	private static ResponseDefinitionBuilder errorResponse(int status, String code, String message) {
		return aResponse()
				.withStatus(status)
				.withHeader("Content-Type", "application/json")
				.withBody("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
	}
}
