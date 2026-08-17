package ru.yandex.practicum.mybank.accounts;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mybank.notifications.outbox.NotificationsOutboxRelay;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresContainerConfig.class)
@EmbeddedKafka(topics = NotificationsEventIntegrationTest.TOPIC, partitions = 1)
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
class NotificationsEventIntegrationTest {

	static final String TOPIC = "notifications";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NotificationsOutboxRelay notificationsOutboxRelay;

	@Autowired
	private EmbeddedKafkaBroker broker;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void publishesCustomerUpdatedEventToTopic() throws Exception {
		mockMvc.perform(put("/api/customers/me")
						.with(jwt().jwt(jwt -> jwt
								.claim("preferred_username", "user1")
								.claim("scope", "customer:write")))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Иванов Игорь", "birthdate": "1990-01-15"}
								"""))
				.andExpect(status().isOk());

		notificationsOutboxRelay.relayPending();

		UUID customerUuid = customerUuidOf("user1");
		UUID eventUuid = outboxEventUuid();

		ConsumerRecord<String, String> record = readRecordOf(eventUuid);

		assertThat(record.key()).isEqualTo(customerUuid.toString());
		JSONAssert.assertEquals("""
				{
					"eventUuid": "%s",
					"type": "customer_updated",
					"recipientUuid": "%s",
					"payload": {"customerUuid": "%s"}
				}""".formatted(eventUuid, customerUuid, customerUuid), record.value(), true);

		assertThat(jdbcTemplate.queryForObject("select status from notifications_outbox", String.class))
				.isEqualTo("PROCESSED");
	}

	private UUID customerUuidOf(String login) {
		return jdbcTemplate.queryForObject("select uuid from customers where login = ?", UUID.class, login);
	}

	private UUID outboxEventUuid() {
		return jdbcTemplate.queryForObject("select uuid from notifications_outbox", UUID.class);
	}

	private ConsumerRecord<String, String> readRecordOf(UUID eventUuid) {
		try (Consumer<String, String> consumer = consumerSubscribedTo(TOPIC)) {
			return StreamSupport.stream(KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10)).spliterator(), false)
					.filter(record -> record.value().contains(eventUuid.toString()))
					.findFirst()
					.orElseThrow(() -> new AssertionError("No record for event " + eventUuid + " in " + TOPIC));
		}
	}

	private Consumer<String, String> consumerSubscribedTo(String topic) {
		Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(broker, "accounts-test-group", true);
		Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(consumerProps,
				new StringDeserializer(), new StringDeserializer()).createConsumer();

		broker.consumeFromEmbeddedTopics(consumer, topic);

		return consumer;
	}
}
