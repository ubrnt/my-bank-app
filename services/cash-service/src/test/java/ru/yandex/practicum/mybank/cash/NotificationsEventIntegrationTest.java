package ru.yandex.practicum.mybank.cash;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.json.JSONException;
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
import ru.yandex.practicum.mybank.cash.client.AccountsClient;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionOperation;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionRequest;
import ru.yandex.practicum.mybank.cash.client.dto.TransactionResponse;
import ru.yandex.practicum.mybank.cash.service.CashService;
import ru.yandex.practicum.mybank.notifications.outbox.NotificationsOutboxRelay;

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

	private static final String ACCOUNT_NUMBER = "40817810000000000001";
	private static final UUID ACCOUNT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID CUSTOMER_UUID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
	private static final UUID IDEMPOTENCY_KEY = UUID.fromString("bbbbbbbb-1111-1111-1111-111111111111");

	@Autowired
	private CashService cashService;

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
		jdbcTemplate.execute("truncate table cash_operations, notifications_outbox");
	}

	@Test
	void publishesDepositEventToTopic() throws JSONException {
		when(accountsClient.deposit(any())).thenAnswer(invocation -> depositResponse(
				invocation.getArgument(0, TransactionRequest.class)));

		cashService.deposit(IDEMPOTENCY_KEY, "user1", 500);
		UUID eventUuid = outboxEventUuid();

		notificationsOutboxRelay.relayPending();

		ConsumerRecord<String, String> record = readRecordOf(eventUuid);

		assertThat(record.key()).isEqualTo(CUSTOMER_UUID.toString());
		JSONAssert.assertEquals("""
				{
					"eventUuid": "%s",
					"type": "money_deposited",
					"recipientUuid": "%s",
					"payload": {
						"transactionUuid": "%s",
						"type": "deposit",
						"operation": {
							"direction": "deposit",
							"toNumber": "%s",
							"toAccountUuid": "%s",
							"toCustomerUuid": "%s",
							"amount": 500,
							"balanceAfter": 25500
						}
					}
				}""".formatted(eventUuid, CUSTOMER_UUID, IDEMPOTENCY_KEY, ACCOUNT_NUMBER, ACCOUNT_UUID, CUSTOMER_UUID),
				record.value(), true);

		assertThat(outboxStatus()).isEqualTo("PROCESSED");
	}

	@Test
	void publishesWithdrawalEventToTopic() throws JSONException {
		when(accountsClient.withdraw(any())).thenAnswer(invocation -> withdrawalResponse(
				invocation.getArgument(0, TransactionRequest.class)));

		cashService.withdraw(IDEMPOTENCY_KEY, "user1", 500);
		UUID eventUuid = outboxEventUuid();

		notificationsOutboxRelay.relayPending();

		ConsumerRecord<String, String> record = readRecordOf(eventUuid);

		assertThat(record.key()).isEqualTo(CUSTOMER_UUID.toString());
		JSONAssert.assertEquals("""
				{
					"eventUuid": "%s",
					"type": "money_withdrawn",
					"recipientUuid": "%s",
					"payload": {
						"transactionUuid": "%s",
						"type": "withdraw",
						"operation": {
							"direction": "withdraw",
							"fromNumber": "%s",
							"fromAccountUuid": "%s",
							"fromCustomerUuid": "%s",
							"amount": 500,
							"balanceAfter": 24500
						}
					}
				}""".formatted(eventUuid, CUSTOMER_UUID, IDEMPOTENCY_KEY, ACCOUNT_NUMBER, ACCOUNT_UUID, CUSTOMER_UUID),
				record.value(), true);

		assertThat(outboxStatus()).isEqualTo("PROCESSED");
	}

	private TransactionResponse depositResponse(TransactionRequest request) {
		TransactionOperation operation = new TransactionOperation("deposit", ACCOUNT_NUMBER, ACCOUNT_UUID,
				CUSTOMER_UUID, null, null, null, request.amount(), 25500L);

		return new TransactionResponse(request.transactionUuid(), "deposit", List.of(operation));
	}

	private TransactionResponse withdrawalResponse(TransactionRequest request) {
		TransactionOperation operation = new TransactionOperation("withdraw", null, null, null, ACCOUNT_NUMBER,
				ACCOUNT_UUID, CUSTOMER_UUID, request.amount(), 24500L);

		return new TransactionResponse(request.transactionUuid(), "withdraw", List.of(operation));
	}

	private UUID outboxEventUuid() {
		return jdbcTemplate.queryForObject("select uuid from notifications_outbox", UUID.class);
	}

	private String outboxStatus() {
		return jdbcTemplate.queryForObject("select status from notifications_outbox", String.class);
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
		Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(broker, "cash-test-group", true);
		Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(consumerProps,
				new StringDeserializer(), new StringDeserializer()).createConsumer();

		broker.consumeFromEmbeddedTopics(consumer, topic);

		return consumer;
	}
}
