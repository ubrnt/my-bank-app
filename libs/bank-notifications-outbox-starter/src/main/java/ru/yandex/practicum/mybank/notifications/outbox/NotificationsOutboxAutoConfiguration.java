package ru.yandex.practicum.mybank.notifications.outbox;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration(after = KafkaAutoConfiguration.class)
@ConditionalOnBean(KafkaTemplate.class)
@EnableConfigurationProperties({NotificationsOutboxProperties.class, NotificationsTopicProperties.class})
@Import(NotificationsOutboxEntityRegistrar.class)
public class NotificationsOutboxAutoConfiguration {

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
	public NewTopic notificationsTopic(NotificationsTopicProperties topicProperties) {
		return TopicBuilder.name(topicProperties.topic())
				.partitions(topicProperties.partitions())
				.replicas(topicProperties.replicas())
				.build();
	}

	@Bean
	public NotificationsEventPublisher notificationsEventPublisher(
			KafkaTemplate<String, NotificationEvent> kafkaTemplate, NotificationsTopicProperties topicProperties,
			NotificationsOutboxProperties properties) {
		return new NotificationsEventPublisher(kafkaTemplate, topicProperties.topic(), properties.sendTimeout());
	}

	@Bean
	public NotificationsOutboxRelay notificationsOutboxRelay(NotificationsOutboxService notificationsOutboxService,
			NotificationsEventPublisher notificationsEventPublisher) {
		return new NotificationsOutboxRelay(notificationsOutboxService, notificationsEventPublisher);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(name = "mybank.notifications.outbox.enabled", matchIfMissing = true)
	@EnableScheduling
	static class SchedulingConfiguration {
	}
}
