package ru.yandex.practicum.mybank.transfer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.mybank.notifications.outbox.NotificationsOutboxRelay;
import ru.yandex.practicum.mybank.transfer.client.AccountsClient;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionOperation;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionRequest;
import ru.yandex.practicum.mybank.transfer.client.dto.TransactionResponse;
import ru.yandex.practicum.mybank.transfer.service.TransferService;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(PostgresContainerConfig.class)
@EmbeddedKafka(topics = NotificationsEventIntegrationTest.TOPIC, partitions = 1)
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
class NotificationsEventIntegrationTest {

	static final String TOPIC = "notifications";

	private static final String FROM_NUMBER = "40817810000000000001";
	private static final UUID FROM_ACCOUNT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID FROM_CUSTOMER_UUID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
	private static final String TO_NUMBER = "40817810000000000002";
	private static final UUID TO_ACCOUNT_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID TO_CUSTOMER_UUID = UUID.fromString("aaaaaaaa-2222-2222-2222-222222222222");
	private static final UUID IDEMPOTENCY_KEY = UUID.fromString("bbbbbbbb-1111-1111-1111-111111111111");

	@Autowired
	private TransferService transferService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NotificationsOutboxRelay notificationsOutboxRelay;

	@Autowired
	private EmbeddedKafkaBroker broker;

	@MockitoBean
	private AccountsClient accountsClient;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@BeforeEach
	void resetData() {
		jdbcTemplate.execute("truncate table transfer_operations, notifications_outbox");
	}

	@Test
	void publishesTransferNotificationEventsToTopic() throws JSONException {
		when(accountsClient.transfer(any())).thenAnswer(invocation ->
				response(invocation.getArgument(0, TransactionRequest.class)));

		transferService.transfer(IDEMPOTENCY_KEY, "user1", "user2", 500);
		UUID receivedEventUuid = outboxEventUuidOf("MONEY_RECEIVED");
		UUID sentEventUuid = outboxEventUuidOf("MONEY_SENT");

		notificationsOutboxRelay.relayPending();

		ConsumerRecord<String, String> sent = readRecordOf(sentEventUuid);

		assertThat(sent.key()).isEqualTo(FROM_CUSTOMER_UUID.toString());
		JSONAssert.assertEquals(expectedEvent(sentEventUuid, "money_sent", FROM_CUSTOMER_UUID, "withdraw", 24500),
				sent.value(), true);

		ConsumerRecord<String, String> received = readRecordOf(receivedEventUuid);

		assertThat(received.key()).isEqualTo(TO_CUSTOMER_UUID.toString());
		JSONAssert.assertEquals(expectedEvent(receivedEventUuid, "money_received", TO_CUSTOMER_UUID, "deposit", 5500),
				received.value(), true);

		assertThat(jdbcTemplate.queryForList("select status from notifications_outbox", String.class))
				.containsExactly("PROCESSED", "PROCESSED");
	}

	private String expectedEvent(UUID eventUuid, String type, UUID recipientUuid, String direction, long balanceAfter) {
		return """
				{
					"eventUuid": "%s",
					"type": "%s",
					"recipientUuid": "%s",
					"payload": {
						"transactionUuid": "%s",
						"type": "transfer",
						"operation": {
							"direction": "%s",
							"fromNumber": "%s",
							"fromAccountUuid": "%s",
							"fromCustomerUuid": "%s",
							"toNumber": "%s",
							"toAccountUuid": "%s",
							"toCustomerUuid": "%s",
							"amount": 500,
							"balanceAfter": %d
						}
					}
				}""".formatted(eventUuid, type, recipientUuid, IDEMPOTENCY_KEY, direction, FROM_NUMBER,
				FROM_ACCOUNT_UUID, FROM_CUSTOMER_UUID, TO_NUMBER, TO_ACCOUNT_UUID, TO_CUSTOMER_UUID, balanceAfter);
	}

	private TransactionResponse response(TransactionRequest request) {
		TransactionOperation sent = new TransactionOperation("withdraw",
				TO_NUMBER, TO_ACCOUNT_UUID, TO_CUSTOMER_UUID,
				FROM_NUMBER, FROM_ACCOUNT_UUID, FROM_CUSTOMER_UUID,
				request.amount(), 24500L);
		TransactionOperation received = new TransactionOperation("deposit",
				TO_NUMBER, TO_ACCOUNT_UUID, TO_CUSTOMER_UUID,
				FROM_NUMBER, FROM_ACCOUNT_UUID, FROM_CUSTOMER_UUID,
				request.amount(), 5500L);

		return new TransactionResponse(request.transactionUuid(), "transfer", List.of(sent, received));
	}

	private UUID outboxEventUuidOf(String eventType) {
		return jdbcTemplate.queryForObject("select uuid from notifications_outbox where event_type = ?", UUID.class,
				eventType);
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
		Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(broker, "transfer-test-group", true);
		Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(consumerProps,
				new StringDeserializer(), new StringDeserializer()).createConsumer();

		broker.consumeFromEmbeddedTopics(consumer, topic);

		return consumer;
	}
}
