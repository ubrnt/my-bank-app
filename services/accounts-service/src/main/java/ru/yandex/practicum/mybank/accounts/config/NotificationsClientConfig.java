package ru.yandex.practicum.mybank.accounts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybank.chassis.client.ServiceClientFactory;

@Configuration
public class NotificationsClientConfig {

	@Bean
	public RestClient notificationsRestClient(ServiceClientFactory factory) {
		return factory.restClient("notifications-service");
	}
}
