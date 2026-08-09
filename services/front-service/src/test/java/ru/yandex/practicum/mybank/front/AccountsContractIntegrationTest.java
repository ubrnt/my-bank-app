package ru.yandex.practicum.mybank.front;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.mybank.chassis.web.FieldError;
import ru.yandex.practicum.mybank.front.client.GatewayClient;
import ru.yandex.practicum.mybank.front.client.GatewayException;
import ru.yandex.practicum.mybank.front.client.dto.CustomerResponse;
import ru.yandex.practicum.mybank.front.client.dto.CustomerSummaryResponse;
import ru.yandex.practicum.mybank.front.client.dto.UpdateProfileRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
	void readsCurrentCustomerCard() {
		CustomerResponse customer = gatewayClient.getCustomer();

		assertThat(customer.login()).isEqualTo("user1");
		assertThat(customer.name()).isNotBlank();
		assertThat(customer.birthdate()).isNotNull();
		assertThat(customer.number()).hasSize(20);
	}

	@Test
	void updatesCurrentCustomerProfile() {
		assertThatNoException().isThrownBy(() ->
				gatewayClient.updateCustomer(new UpdateProfileRequest("Иванов Иван", LocalDate.of(1990, 1, 15))));
	}

	@Test
	void readsFieldErrorsWhenTheProfileIsRejected() {
		UpdateProfileRequest underage = new UpdateProfileRequest("Иванов Иван", LocalDate.of(2020, 1, 15));

		assertThatThrownBy(() -> gatewayClient.updateCustomer(underage))
				.isInstanceOf(GatewayException.class)
				.extracting(exception -> ((GatewayException) exception).getResponse())
				.satisfies(response -> {
					assertThat(response.code()).isEqualTo("validation_error");
					assertThat(response.validationErrors().fields()).extracting(FieldError::field)
							.containsExactly("birthdate");
				});
	}

	@Test
	void readsOtherCustomers() {
		List<CustomerSummaryResponse> others = gatewayClient.getOtherCustomers();

		assertThat(others).isNotEmpty();
		assertThat(others.getFirst().login()).isNotBlank();
		assertThat(others.getFirst().name()).isNotBlank();
	}
}
