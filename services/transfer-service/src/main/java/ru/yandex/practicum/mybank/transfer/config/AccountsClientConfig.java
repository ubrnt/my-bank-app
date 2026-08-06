package ru.yandex.practicum.mybank.transfer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybank.chassis.client.ServiceClientFactory;

@Configuration
public class AccountsClientConfig {

	@Bean
	public RestClient accountsRestClient(ServiceClientFactory factory) {
		return factory.restClient("accounts-service");
	}
}
