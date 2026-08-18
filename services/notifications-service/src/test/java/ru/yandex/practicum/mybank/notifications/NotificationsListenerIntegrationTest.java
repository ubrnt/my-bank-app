package ru.yandex.practicum.mybank.notifications;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.mybank.notifications.client.AccountsClient;
import ru.yandex.practicum.mybank.notifications.client.dto.CustomerResponse;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(PostgresContainerConfig.class)
@EmbeddedKafka(topics = NotificationsListenerIntegrationTest.TOPIC, partitions = 1)
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
class NotificationsListenerIntegrationTest {

	static final String TOPIC = "notifications";

	private static final UUID RECIPIENT_UUID = UUID.fromString("3f2a77c4-1e08-4a6b-8f21-9c0d5b7e1111");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@MockitoBean
	private AccountsClient accountsClient;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@BeforeEach
	void stubRecipient() {
		when(accountsClient.getCustomer(any())).thenReturn(new CustomerResponse("user1", "Иванов Игорь"));
	}

	@Test
	void storesNotificationReadFromTopic() {
		UUID eventUuid = UUID.randomUUID();

		send(depositEvent(eventUuid));

		Map<String, Object> notification = awaitNotificationOf(eventUuid);

		assertThat(notification.get("customer_uuid")).isEqualTo(RECIPIENT_UUID);
		assertThat(notification.get("type")).isEqualTo("MONEY_DEPOSITED");
		assertThat(notification.get("message")).isEqualTo("Счёт *0001: пополнение на 500. Доступно 25\u00A0500");
	}

	@Test
	void ignoresRepeatedEvent() {
		UUID eventUuid = UUID.randomUUID();

		UUID nextEventUuid = UUID.randomUUID();

		send(depositEvent(eventUuid));
		send(depositEvent(eventUuid));
		send(depositEvent(nextEventUuid));

		awaitNotificationOf(nextEventUuid);

		assertThat(countOfNotificationsOf(eventUuid)).isOne();
	}

	@Test
	void skipsBrokenEventAndKeepsReading() {
		UUID brokenEventUuid = UUID.randomUUID();
		UUID validEventUuid = UUID.randomUUID();

		send("""
				{
					"eventUuid": "%s",
					"type": "money_deposited",
					"recipientUuid": "%s",
					"payload": {"operation": {"toNumber": "40817810000000000001"}}
				}""".formatted(brokenEventUuid, RECIPIENT_UUID));
		send(depositEvent(validEventUuid));

		awaitNotificationOf(validEventUuid);

		assertThat(countOfNotificationsOf(brokenEventUuid)).isZero();
	}

	private String depositEvent(UUID eventUuid) {
		return """
				{
					"eventUuid": "%s",
					"type": "money_deposited",
					"recipientUuid": "%s",
					"payload": {
						"operation": {
							"toNumber": "40817810000000000001",
							"amount": 500,
							"balanceAfter": 25500
						}
					}
				}""".formatted(eventUuid, RECIPIENT_UUID);
	}

	private void send(String event) {
		kafkaTemplate.send(TOPIC, RECIPIENT_UUID.toString(), event);
	}

	private Map<String, Object> awaitNotificationOf(UUID eventUuid) {
		await().atMost(Duration.ofSeconds(20)).until(() -> countOfNotificationsOf(eventUuid) == 1);

		return jdbcTemplate.queryForMap("select * from notifications where event_uuid = ?", eventUuid);
	}

	private long countOfNotificationsOf(UUID eventUuid) {
		return jdbcTemplate.queryForObject("select count(*) from notifications where event_uuid = ?", Long.class,
				eventUuid);
	}
}
