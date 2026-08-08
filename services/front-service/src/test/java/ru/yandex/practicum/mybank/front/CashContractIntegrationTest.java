package ru.yandex.practicum.mybank.front;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.mybank.front.client.GatewayClient;
import ru.yandex.practicum.mybank.front.client.GatewayException;
import ru.yandex.practicum.mybank.front.client.dto.CashRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties =
		"spring.cloud.discovery.client.simple.instances.gateway-service[0].uri=http://localhost:8091")
@AutoConfigureStubRunner(
		ids = "ru.yandex.practicum:cash-service:+:stubs:8091",
		stubsMode = StubRunnerProperties.StubsMode.LOCAL)
@Import(FakeTokenConfig.class)
class CashContractIntegrationTest {

	private static final UUID IN_PROGRESS_KEY = UUID.fromString("dddddddd-1111-1111-1111-111111111111");

	@Autowired
	private GatewayClient gatewayClient;

	@Test
	void depositRequestMatchesTheCashContract() {
		assertThatCode(() -> gatewayClient.deposit(UUID.randomUUID(), new CashRequest(1500)))
				.doesNotThrowAnyException();
	}

	@Test
	void withdrawalRequestMatchesTheCashContract() {
		assertThatCode(() -> gatewayClient.withdraw(UUID.randomUUID(), new CashRequest(500)))
				.doesNotThrowAnyException();
	}

	@Test
	void withdrawalBeyondBalanceIsRejectedByTheCashContract() {
		assertThatThrownBy(() -> gatewayClient.withdraw(UUID.randomUUID(), new CashRequest(1_000_000_000_000L)))
				.isInstanceOfSatisfying(GatewayException.class, exception -> {
					assertThat(exception.getResponse()).isNotNull();
					assertThat(exception.getResponse().code()).isEqualTo("insufficient_funds");
				});
	}

	@Test
	void repeatedRequestIsRejectedByTheCashContract() {
		assertThatThrownBy(() -> gatewayClient.deposit(IN_PROGRESS_KEY, new CashRequest(1500)))
				.isInstanceOfSatisfying(GatewayException.class, exception -> {
					assertThat(exception.getResponse()).isNotNull();
					assertThat(exception.getResponse().code()).isEqualTo("duplicate_request");
				});
	}
}
