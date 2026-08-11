package ru.yandex.practicum.mybank.front;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.mybank.front.client.GatewayClient;
import ru.yandex.practicum.mybank.front.client.GatewayException;
import ru.yandex.practicum.mybank.front.client.dto.TransferRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties =
		"spring.cloud.discovery.client.simple.instances.gateway-service[0].uri=http://localhost:8092")
@AutoConfigureStubRunner(
		ids = "ru.yandex.practicum:transfer-service:+:stubs:8092",
		stubsMode = StubRunnerProperties.StubsMode.LOCAL)
@Import(FakeTokenConfig.class)
class TransferContractIntegrationTest {

	private static final UUID IN_PROGRESS_KEY = UUID.fromString("eeeeeeee-1111-1111-1111-111111111111");

	@Autowired
	private GatewayClient gatewayClient;

	@Test
	void transferRequestMatchesTheTransferContract() {
		assertThatCode(() -> gatewayClient.transfer(UUID.randomUUID(), new TransferRequest("user2", 500)))
				.doesNotThrowAnyException();
	}

	@Test
	void transferBeyondBalanceIsRejectedByTheTransferContract() {
		assertThatThrownBy(() ->
				gatewayClient.transfer(UUID.randomUUID(), new TransferRequest("user2", 1_000_000_000_000L)))
				.isInstanceOfSatisfying(GatewayException.class, exception -> {
					assertThat(exception.getResponse()).isNotNull();
					assertThat(exception.getResponse().code()).isEqualTo("insufficient_funds");
				});
	}

	@Test
	void transferToSelfIsRejectedByTheTransferContract() {
		assertThatThrownBy(() -> gatewayClient.transfer(UUID.randomUUID(), new TransferRequest("user1", 500)))
				.isInstanceOfSatisfying(GatewayException.class, exception -> {
					assertThat(exception.getResponse()).isNotNull();
					assertThat(exception.getResponse().code()).isEqualTo("same_account");
				});
	}

	@Test
	void repeatedRequestIsRejectedByTheTransferContract() {
		assertThatThrownBy(() -> gatewayClient.transfer(IN_PROGRESS_KEY, new TransferRequest("user2", 500)))
				.isInstanceOfSatisfying(GatewayException.class, exception -> {
					assertThat(exception.getResponse()).isNotNull();
					assertThat(exception.getResponse().code()).isEqualTo("duplicate_request");
				});
	}
}
