package ru.yandex.practicum.mybank.notifications.outbox;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig
@ImportAutoConfiguration(KafkaAutoConfiguration.class)
@EmbeddedKafka(topics = NotificationsEventPublisherTest.TOPIC, partitions = 1)
@TestPropertySource(properties = {
		"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
		"spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
		"spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer",
		"spring.kafka.producer.properties.spring.json.add.type.headers=false"
})
class NotificationsEventPublisherTest {

	static final String TOPIC = "notifications-test";

	private static final UUID EVENT_UUID = UUID.fromString("11110000-2222-4333-8444-555566660001");
	private static final UUID RECIPIENT_UUID = UUID.fromString("3f2a77c4-1e08-4a6b-8f21-9c0d5b7e1111");
	private static final String PAYLOAD_JSON = "{\"operation\":{\"amount\":500}}";

	@Autowired
	private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

	@Autowired
	private EmbeddedKafkaBroker broker;

	@Test
	void publishesEventKeyedByRecipient() {
		NotificationsEventPublisher publisher = new NotificationsEventPublisher(kafkaTemplate, TOPIC,
				Duration.ofSeconds(10));

		publisher.send(new NotificationEvent(EVENT_UUID, "money_deposited", RECIPIENT_UUID, PAYLOAD_JSON));

		ConsumerRecord<String, String> record = readSingleRecordFrom(TOPIC);

		assertThat(record.key()).isEqualTo(RECIPIENT_UUID.toString());
		assertThat(record.headers().toArray()).isEmpty();
		assertThat(record.value()).isEqualToIgnoringWhitespace("""
				{
					"eventUuid": "11110000-2222-4333-8444-555566660001",
					"type": "money_deposited",
					"recipientUuid": "3f2a77c4-1e08-4a6b-8f21-9c0d5b7e1111",
					"payload": {"operation":{"amount":500}}
				}""");
	}

	@Test
	void failsWhenBrokerIsUnavailable() {
		Map<String, Object> unreachableBrokerProps = Map.of(
				ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:1",
				ProducerConfig.MAX_BLOCK_MS_CONFIG, 500);
		DefaultKafkaProducerFactory<String, NotificationEvent> producerFactory = new DefaultKafkaProducerFactory<>(
				unreachableBrokerProps, new StringSerializer(), new JacksonJsonSerializer<>());

		try {
			NotificationsEventPublisher publisher = new NotificationsEventPublisher(
					new KafkaTemplate<>(producerFactory), TOPIC, Duration.ofSeconds(2));

			assertThatThrownBy(() -> publisher.send(
					new NotificationEvent(EVENT_UUID, "money_deposited", RECIPIENT_UUID, PAYLOAD_JSON)))
					.isInstanceOf(NotificationDeliveryException.class)
					.hasMessageContaining(EVENT_UUID.toString());
		} finally {
			producerFactory.destroy();
		}
	}

	private ConsumerRecord<String, String> readSingleRecordFrom(String topic) {
		try (Consumer<String, String> consumer = consumerSubscribedTo(topic)) {
			return KafkaTestUtils.getSingleRecord(consumer, topic, Duration.ofSeconds(10));
		}
	}

	private Consumer<String, String> consumerSubscribedTo(String topic) {
		Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(broker, "test-group", true);
		Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(consumerProps,
				new StringDeserializer(), new StringDeserializer()).createConsumer();

		broker.consumeFromEmbeddedTopics(consumer, topic);

		return consumer;
	}
}
