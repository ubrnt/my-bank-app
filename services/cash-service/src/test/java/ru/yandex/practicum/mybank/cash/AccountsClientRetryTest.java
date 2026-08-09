package ru.yandex.practicum.mybank.cash;

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
import ru.yandex.practicum.mybank.cash.client.AccountsClient;
import ru.yandex.practicum.mybank.cash.client.TransactionRejectedException;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionRequest;
import ru.yandex.practicum.mybank.chassis.client.ServiceCallException;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "mybank.accounts.retry.delay=1ms")
@Import({PostgresContainerConfig.class, FakeTokenConfig.class})
class AccountsClientRetryTest {

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
	void serverErrorIsRetriedUntilAttemptsRunOut() {
		accounts.stubFor(post(urlEqualTo("/api/transactions/deposit"))
				.willReturn(aResponse().withStatus(500)));

		assertThatThrownBy(() -> accountsClient.deposit(new TransactionRequest(UUID.randomUUID(), "user1", 500)))
				.isInstanceOf(ServiceCallException.class);

		accounts.verify(2, postRequestedFor(urlEqualTo("/api/transactions/deposit")));
	}

	@Test
	void rejectedTransactionIsNotRetried() {
		accounts.stubFor(post(urlEqualTo("/api/transactions/withdraw"))
				.willReturn(aResponse()
						.withStatus(422)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"code\":\"insufficient_funds\",\"message\":\"Not enough money\"}")));

		assertThatThrownBy(() -> accountsClient.withdraw(new TransactionRequest(UUID.randomUUID(), "user1", 500)))
				.isInstanceOf(TransactionRejectedException.class);

		accounts.verify(1, postRequestedFor(urlEqualTo("/api/transactions/withdraw")));
	}

	@Test
	void conflictingTransactionIsReportedAsRejection() {
		accounts.stubFor(post(urlEqualTo("/api/transactions/deposit"))
				.willReturn(aResponse()
						.withStatus(409)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"code\":\"transaction_conflict\",\"message\":\"Already applied with other details\"}")));

		assertThatThrownBy(() -> accountsClient.deposit(new TransactionRequest(UUID.randomUUID(), "user1", 500)))
				.isInstanceOf(TransactionRejectedException.class)
				.hasFieldOrPropertyWithValue("code", "transaction_conflict");

		accounts.verify(1, postRequestedFor(urlEqualTo("/api/transactions/deposit")));
	}
}
