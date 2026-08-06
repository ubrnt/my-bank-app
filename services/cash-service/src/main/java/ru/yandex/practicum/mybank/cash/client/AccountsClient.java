package ru.yandex.practicum.mybank.cash.client;

import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionRequest;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionResponse;
import ru.yandex.practicum.mybank.chassis.client.ServiceCallException;
import ru.yandex.practicum.mybank.chassis.web.ErrorResponse;

@Component
public class AccountsClient {

	private final RestClient restClient;

	public AccountsClient(RestClient accountsRestClient) {
		this.restClient = accountsRestClient;
	}

	@Retryable(includes = ServiceCallException.class,
			maxRetriesString = "${mybank.accounts.retry.max-retries:2}",
			delayString = "${mybank.accounts.retry.delay:200ms}",
			multiplierString = "${mybank.accounts.retry.multiplier:2}")
	public TransactionResponse deposit(TransactionRequest request) {
		return post("/api/transactions/deposit", request);
	}

	@Retryable(includes = ServiceCallException.class,
			maxRetriesString = "${mybank.accounts.retry.max-retries:2}",
			delayString = "${mybank.accounts.retry.delay:200ms}",
			multiplierString = "${mybank.accounts.retry.multiplier:2}")
	public TransactionResponse withdraw(TransactionRequest request) {
		return post("/api/transactions/withdraw", request);
	}

	private TransactionResponse post(String uri, TransactionRequest request) {
		try {
			return restClient.post()
					.uri(uri)
					.contentType(MediaType.APPLICATION_JSON)
					.body(request)
					.retrieve()
					.body(TransactionResponse.class);
		} catch (HttpClientErrorException.UnprocessableContent e) {
			ErrorResponse error = e.getResponseBodyAs(ErrorResponse.class);

			throw new TransactionRejectedException(error.code(), error.message());
		}
	}
}
