package ru.yandex.practicum.mybank.chassis.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientsPropertiesTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(EnabledProperties.class);

	@Test
	void bindsBaseUrlsFromConfiguration() {
		runner.withPropertyValues("mybank.clients.base-urls.accounts-service=http://accounts-service:8082")
				.run(context -> assertThat(context.getBean(ClientsProperties.class).baseUrl("accounts-service"))
						.isEqualTo("http://accounts-service:8082"));
	}

	@Test
	void toleratesConfigurationWithoutAnyClients() {
		runner.run(context -> assertThat(context.getBean(ClientsProperties.class).baseUrls()).isEmpty());
	}

	@Test
	void rejectsAnUnconfiguredServiceId() {
		ClientsProperties properties = new ClientsProperties(Map.of());

		assertThatThrownBy(() -> properties.baseUrl("accounts-service"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("mybank.clients.base-urls.accounts-service");
	}

	@Test
	void rejectsABlankBaseUrl() {
		ClientsProperties properties = new ClientsProperties(Map.of("accounts-service", " "));

		assertThatThrownBy(() -> properties.baseUrl("accounts-service"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Configuration
	@EnableConfigurationProperties(ClientsProperties.class)
	static class EnabledProperties {
	}
}
