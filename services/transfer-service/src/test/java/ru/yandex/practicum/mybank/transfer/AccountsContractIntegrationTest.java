package ru.yandex.practicum.mybank.transfer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.mybank.transfer.client.AccountsClient;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionRequest;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "mybank.clients.base-urls.accounts-service=http://localhost:8096")
@AutoConfigureStubRunner(
		ids = "ru.yandex.practicum:accounts-service:+:stubs:8096",
		stubsMode = StubRunnerProperties.StubsMode.LOCAL)
@Import({PostgresContainerConfig.class, FakeTokenConfig.class})
class AccountsContractIntegrationTest {

	private static final UUID FROM_ACCOUNT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID FROM_CUSTOMER_UUID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
	private static final UUID TO_ACCOUNT_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID TO_CUSTOMER_UUID = UUID.fromString("aaaaaaaa-2222-2222-2222-222222222222");

	@Autowired
	private AccountsClient accountsClient;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void transferReturnsBothSidesOfTheTransaction() {
		UUID transactionUuid = UUID.randomUUID();

		TransactionResponse response = accountsClient.transfer(
				new TransactionRequest(transactionUuid, "user1", "user2", 500));

		assertThat(response.uuid()).isEqualTo(transactionUuid);
		assertThat(response.operations()).hasSize(2);

		assertThat(response.sent().fromAccountUuid()).isEqualTo(FROM_ACCOUNT_UUID);
		assertThat(response.sent().fromCustomerUuid()).isEqualTo(FROM_CUSTOMER_UUID);
		assertThat(response.sent().toAccountUuid()).isEqualTo(TO_ACCOUNT_UUID);
		assertThat(response.sent().toCustomerUuid()).isEqualTo(TO_CUSTOMER_UUID);

		assertThat(response.received().balanceAfter()).isNotEqualTo(response.sent().balanceAfter());
	}
}
