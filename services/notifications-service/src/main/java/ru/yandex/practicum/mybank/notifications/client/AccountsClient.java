package ru.yandex.practicum.mybank.notifications.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.yandex.practicum.mybank.notifications.client.dto.CustomerResponse;

import java.util.UUID;

@Component
public class AccountsClient {

	private final RestClient restClient;

	public AccountsClient(RestClient accountsRestClient) {
		this.restClient = accountsRestClient;
	}

	public CustomerResponse getCustomer(UUID customerUuid) {
		try {
			return restClient.get()
					.uri("/api/customers/{uuid}", customerUuid)
					.retrieve()
					.body(CustomerResponse.class);
		} catch (HttpClientErrorException.NotFound e) {
			throw new UnknownRecipientException(customerUuid, e);
		} catch (RestClientException e) {
			throw new CustomerResolutionException(customerUuid, e);
		}
	}
}
