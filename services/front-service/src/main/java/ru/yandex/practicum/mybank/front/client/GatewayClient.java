package ru.yandex.practicum.mybank.front.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.yandex.practicum.mybank.front.client.dto.CashRequest;
import ru.yandex.practicum.mybank.front.client.dto.CustomerResponse;
import ru.yandex.practicum.mybank.front.client.dto.CustomerSummaryResponse;
import ru.yandex.practicum.mybank.front.client.dto.ErrorResponse;
import ru.yandex.practicum.mybank.front.client.dto.TransferRequest;
import ru.yandex.practicum.mybank.front.client.dto.UpdateProfileRequest;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class GatewayClient {

	private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

	private final RestClient restClient;

	public GatewayClient(RestClient gatewayRestClient) {
		this.restClient = gatewayRestClient;
	}

	public CustomerResponse getCustomer() {
		return call(() -> restClient.get()
				.uri("/api/customers/me")
				.retrieve()
				.body(CustomerResponse.class));
	}

	public List<CustomerSummaryResponse> getOtherCustomers() {
		return call(() -> restClient.get()
				.uri("/api/customers/others")
				.retrieve()
				.body(new ParameterizedTypeReference<>() {}));
	}

	public void updateCustomer(UpdateProfileRequest request) {
		call(() -> restClient.put()
				.uri("/api/customers/me")
				.body(request)
				.retrieve()
				.toBodilessEntity());
	}

	public void deposit(UUID idempotencyKey, CashRequest request) {
		call(() -> restClient.post()
				.uri("/api/cash/deposit")
				.header(IDEMPOTENCY_KEY, idempotencyKey.toString())
				.body(request)
				.retrieve()
				.toBodilessEntity());
	}

	public void withdraw(UUID idempotencyKey, CashRequest request) {
		call(() -> restClient.post()
				.uri("/api/cash/withdraw")
				.header(IDEMPOTENCY_KEY, idempotencyKey.toString())
				.body(request)
				.retrieve()
				.toBodilessEntity());
	}

	public void transfer(UUID idempotencyKey, TransferRequest request) {
		call(() -> restClient.post()
				.uri("/api/transfers")
				.header(IDEMPOTENCY_KEY, idempotencyKey.toString())
				.body(request)
				.retrieve()
				.toBodilessEntity());
	}

	private <T> T call(Supplier<T> request) {
		try {
			return request.get();
		} catch (RestClientResponseException exception) {
			throw new GatewayException(exception.getResponseBodyAs(ErrorResponse.class), exception);
		} catch (RestClientException exception) {
			throw new GatewayException(null, exception);
		}
	}
}
