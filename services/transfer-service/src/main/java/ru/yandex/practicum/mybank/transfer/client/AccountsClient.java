package ru.yandex.practicum.mybank.transfer.client;

import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybank.chassis.client.ServiceCallException;
import ru.yandex.practicum.mybank.chassis.web.ErrorResponse;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionRequest;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionResponse;

@Component
public class AccountsClient {

	private final RestClient restClient;

	public AccountsClient(RestClient accountsRestClient) {
		this.restClient = accountsRestClient;
	}

	@Retryable(includes = ServiceCallException.class,
			maxRetriesString = "${mybank.accounts.retry.max-retries:1}",
			delayString = "${mybank.accounts.retry.delay:200ms}",
			multiplierString = "${mybank.accounts.retry.multiplier:2}")
	public TransactionResponse transfer(TransactionRequest request) {
		try {
			return restClient.post()
					.uri("/api/transactions/transfer")
					.contentType(MediaType.APPLICATION_JSON)
					.body(request)
					.retrieve()
					.body(TransactionResponse.class);
		} catch (HttpClientErrorException.NotFound
				| HttpClientErrorException.UnprocessableContent
				| HttpClientErrorException.Conflict e) {
			ErrorResponse error = e.getResponseBodyAs(ErrorResponse.class);

			if (error == null) {
				throw new TransactionRejectedException(TransactionRejectedException.UNREADABLE_REJECTION,
						"accounts-service rejected the transfer with status " + e.getStatusCode());
			}

			throw new TransactionRejectedException(error.code(), error.message());
		}
	}
}
