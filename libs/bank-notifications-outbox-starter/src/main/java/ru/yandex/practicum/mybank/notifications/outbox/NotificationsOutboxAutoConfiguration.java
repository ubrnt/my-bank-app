package ru.yandex.practicum.mybank.notifications.outbox;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.yandex.practicum.mybank.chassis.client.ClientAutoConfiguration;
import ru.yandex.practicum.mybank.chassis.client.ServiceClientFactory;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration(after = ClientAutoConfiguration.class)
@ConditionalOnBean(ServiceClientFactory.class)
@EnableConfigurationProperties(NotificationsOutboxProperties.class)
@Import(NotificationsOutboxEntityRegistrar.class)
public class NotificationsOutboxAutoConfiguration {

	private static final String NOTIFICATIONS_SERVICE_ID = "notifications-service";

	@Bean
	public NotificationsOutboxEventRepository notificationsOutboxEventRepository() {
		return new NotificationsOutboxEventRepository();
	}

	@Bean
	public NotificationsOutboxService notificationsOutboxService(
			NotificationsOutboxEventRepository notificationsOutboxEventRepository, ObjectMapper objectMapper,
			NotificationsOutboxProperties properties) {
		return new NotificationsOutboxService(notificationsOutboxEventRepository, objectMapper, properties);
	}

	@Bean
	public NotificationsClient notificationsClient(ServiceClientFactory factory) {
		return new NotificationsClient(factory.restClient(NOTIFICATIONS_SERVICE_ID));
	}

	@Bean
	public NotificationsOutboxRelay notificationsOutboxRelay(NotificationsOutboxService notificationsOutboxService,
			NotificationsClient notificationsClient) {
		return new NotificationsOutboxRelay(notificationsOutboxService, notificationsClient);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(name = "mybank.notifications.outbox.enabled", matchIfMissing = true)
	@EnableScheduling
	static class SchedulingConfiguration {
	}
}
