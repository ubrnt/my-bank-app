package ru.yandex.practicum.mybank.cash;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.mybank.cash.client.AccountsClient;
import ru.yandex.practicum.mybank.cash.client.TransactionRejectedException;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionRequest;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "mybank.clients.base-urls.accounts-service=http://localhost:8094")
@AutoConfigureStubRunner(
		ids = "ru.yandex.practicum:accounts-service:+:stubs:8094",
		stubsMode = StubRunnerProperties.StubsMode.LOCAL)
@Import({PostgresContainerConfig.class, FakeTokenConfig.class})
class AccountsContractIntegrationTest {

	private static final UUID ACCOUNT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID CUSTOMER_UUID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");

	@Autowired
	private AccountsClient accountsClient;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void depositReturnsToAccountAndCustomer() {
		UUID transactionUuid = UUID.randomUUID();

		TransactionResponse response = accountsClient.deposit(new TransactionRequest(transactionUuid, "user1", 500));

		assertThat(response.uuid()).isEqualTo(transactionUuid);
		assertThat(response.operation().toAccountUuid()).isEqualTo(ACCOUNT_UUID);
		assertThat(response.operation().toCustomerUuid()).isEqualTo(CUSTOMER_UUID);
	}

	@Test
	void withdrawReturnsFromAccountAndCustomer() {
		UUID transactionUuid = UUID.randomUUID();

		TransactionResponse response = accountsClient.withdraw(new TransactionRequest(transactionUuid, "user1", 500));

		assertThat(response.uuid()).isEqualTo(transactionUuid);
		assertThat(response.operation().fromAccountUuid()).isEqualTo(ACCOUNT_UUID);
		assertThat(response.operation().fromCustomerUuid()).isEqualTo(CUSTOMER_UUID);
	}

	@Test
	void withdrawalBeyondBalanceIsRejected() {
		TransactionRequest request = new TransactionRequest(UUID.randomUUID(), "user1", 1_000_000_000_000L);

		assertThatThrownBy(() -> accountsClient.withdraw(request))
				.isInstanceOf(TransactionRejectedException.class)
				.extracting(e -> ((TransactionRejectedException) e).getCode())
				.isEqualTo("insufficient_funds");
	}
}
