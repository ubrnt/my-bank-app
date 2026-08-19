package ru.yandex.practicum.mybank.notifications.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("mybank.notifications")
public record NotificationsTopicProperties(
		@DefaultValue("notifications") String topic,
		@DefaultValue("3") int partitions,
		@DefaultValue("1") short replicas
) {
}
