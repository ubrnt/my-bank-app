package ru.yandex.practicum.mybank.front;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.mybank.front.client.GatewayClient;
import ru.yandex.practicum.mybank.front.client.dto.CustomerResponse;
import ru.yandex.practicum.mybank.front.client.dto.CustomerSummaryResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties =
		"spring.cloud.discovery.client.simple.instances.gateway-service[0].uri=http://localhost:8090")
@AutoConfigureStubRunner(
		ids = "ru.yandex.practicum:accounts-service:+:stubs:8090",
		stubsMode = StubRunnerProperties.StubsMode.LOCAL)
@Import(FakeTokenConfig.class)
class AccountsContractIntegrationTest {

	@Autowired
	private GatewayClient gatewayClient;

	@Test
	void readsTheCardOfTheLoggedInCustomer() {
		CustomerResponse customer = gatewayClient.getCustomer();

		assertThat(customer.login()).isEqualTo("user1");
		assertThat(customer.name()).isNotBlank();
		assertThat(customer.birthdate()).isNotNull();
		assertThat(customer.number()).hasSize(20);
	}

	@Test
	void readsOtherCustomers() {
		List<CustomerSummaryResponse> others = gatewayClient.getOtherCustomers();

		assertThat(others).isNotEmpty();
		assertThat(others.getFirst().login()).isNotBlank();
		assertThat(others.getFirst().name()).isNotBlank();
	}
}
