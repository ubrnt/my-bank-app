package ru.yandex.practicum.mybank.chassis.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties("mybank.clients")
public record ClientsProperties(
		Map<String, String> baseUrls
) {

	public ClientsProperties {
		baseUrls = baseUrls == null ? Map.of() : Map.copyOf(baseUrls);
	}

	public String baseUrl(String serviceId) {
		String baseUrl = baseUrls.get(serviceId);

		if (baseUrl == null || baseUrl.isBlank()) {
			throw new IllegalStateException(
					"No base URL configured for " + serviceId + ", set mybank.clients.base-urls." + serviceId);
		}

		return baseUrl;
	}
}
