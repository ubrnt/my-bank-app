package ru.yandex.practicum.mybank.cash;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.mybank.cash.client.AccountsClient;
import ru.yandex.practicum.mybank.cash.client.TransactionRejectedException;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionRequest;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionResponse;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureStubRunner(
		ids = "ru.yandex.practicum:accounts-service:+:stubs",
		stubsMode = StubRunnerProperties.StubsMode.LOCAL)
@Import({PostgresContainerConfig.class, AccountsContractIntegrationTest.FakeTokenConfig.class})
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

	@TestConfiguration(proxyBeanMethods = false)
	static class FakeTokenConfig {

		@Bean
		@Primary
		public OAuth2AuthorizedClientManager fakeAuthorizedClientManager(ClientRegistrationRepository registrations) {
			ClientRegistration registration = registrations.findByRegistrationId("accounts-service");
			OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
					"fake-token", Instant.now(), Instant.now().plusSeconds(3600));

			return request -> new OAuth2AuthorizedClient(registration, "cash-service", token);
		}
	}
}
