package ru.yandex.practicum.mybank.transfer.contracts;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.yandex.practicum.mybank.contract.kafka.KafkaContractMessagingConfiguration;
import ru.yandex.practicum.mybank.notifications.outbox.NotificationEvent;
import ru.yandex.practicum.mybank.notifications.outbox.NotificationsEventPublisher;

import java.time.Duration;
import java.util.UUID;

@SpringJUnitConfig(NotificationEventsBase.PublisherConfig.class)
@ImportAutoConfiguration(KafkaAutoConfiguration.class)
@AutoConfigureMessageVerifier
@EmbeddedKafka(topics = NotificationEventsBase.TOPIC, partitions = 1)
@TestPropertySource(properties = {
		"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
		"spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
		"spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer",
		"spring.kafka.producer.properties.spring.json.add.type.headers=false"
})
public abstract class NotificationEventsBase {

	static final String TOPIC = "notifications";

	private static final UUID EVENT_UUID = UUID.fromString("1b7f4a90-0d51-4c2e-9f77-0a1e5c3b0003");
	private static final UUID TRANSACTION_UUID = UUID.fromString("bbbbbbbb-1111-1111-1111-111111111111");
	private static final UUID FROM_ACCOUNT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID FROM_CUSTOMER_UUID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
	private static final UUID TO_ACCOUNT_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID TO_CUSTOMER_UUID = UUID.fromString("aaaaaaaa-2222-2222-2222-222222222222");
	private static final String FROM_NUMBER = "40817810000000000001";
	private static final String TO_NUMBER = "40817810000000000002";

	@Autowired
	private NotificationsEventPublisher notificationsEventPublisher;

	public void publishMoneySent() {
		String payload = """
				{
					"transactionUuid": "%s",
					"type": "transfer",
					"operation": {
						"direction": "withdraw",
						"fromNumber": "%s",
						"fromAccountUuid": "%s",
						"fromCustomerUuid": "%s",
						"toNumber": "%s",
						"toAccountUuid": "%s",
						"toCustomerUuid": "%s",
						"amount": 500,
						"balanceAfter": 24500
					}
				}""".formatted(TRANSACTION_UUID, FROM_NUMBER, FROM_ACCOUNT_UUID, FROM_CUSTOMER_UUID,
				TO_NUMBER, TO_ACCOUNT_UUID, TO_CUSTOMER_UUID);

		notificationsEventPublisher.send(
				new NotificationEvent(EVENT_UUID, "money_sent", FROM_CUSTOMER_UUID, payload));
	}

	@Configuration
	@Import(KafkaContractMessagingConfiguration.class)
	static class PublisherConfig {

		@Bean
		public NotificationsEventPublisher notificationsEventPublisher(
				KafkaTemplate<String, NotificationEvent> kafkaTemplate) {
			return new NotificationsEventPublisher(kafkaTemplate, TOPIC, Duration.ofSeconds(10));
		}
	}
}
