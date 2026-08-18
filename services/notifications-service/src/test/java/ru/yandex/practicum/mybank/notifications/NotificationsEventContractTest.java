package ru.yandex.practicum.mybank.notifications;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.StubTrigger;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.mybank.contract.kafka.KafkaContractMessagingConfiguration;
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
@Import({PostgresContainerConfig.class, KafkaContractMessagingConfiguration.class})
@EmbeddedKafka(topics = NotificationsEventContractTest.TOPIC, partitions = 1)
@AutoConfigureStubRunner(
		ids = {
				"ru.yandex.practicum:accounts-service:+:stubs",
				"ru.yandex.practicum:cash-service:+:stubs",
				"ru.yandex.practicum:transfer-service:+:stubs"
		},
		stubsMode = StubRunnerProperties.StubsMode.LOCAL)
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
class NotificationsEventContractTest {

	static final String TOPIC = "notifications";

	private static final UUID CUSTOMER_UPDATED_EVENT_UUID = UUID.fromString("1b7f4a90-0d51-4c2e-9f77-0a1e5c3b0001");
	private static final UUID MONEY_DEPOSITED_EVENT_UUID = UUID.fromString("1b7f4a90-0d51-4c2e-9f77-0a1e5c3b0002");
	private static final UUID MONEY_SENT_EVENT_UUID = UUID.fromString("1b7f4a90-0d51-4c2e-9f77-0a1e5c3b0003");
	private static final UUID CUSTOMER_UUID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");

	@Autowired
	private StubTrigger stubTrigger;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private AccountsClient accountsClient;

	@BeforeEach
	void stubRecipient() {
		when(accountsClient.getCustomer(any())).thenReturn(new CustomerResponse("user1", "Иванов Игорь"));
	}

	@Test
	void storesEventPublishedByAccounts() {
		stubTrigger.trigger("customer_updated_event");

		assertThat(awaitNotificationOf(CUSTOMER_UPDATED_EVENT_UUID))
				.containsEntry("customer_uuid", CUSTOMER_UUID)
				.containsEntry("type", "CUSTOMER_UPDATED");
	}

	@Test
	void storesEventPublishedByCash() {
		stubTrigger.trigger("money_deposited_event");

		assertThat(awaitNotificationOf(MONEY_DEPOSITED_EVENT_UUID))
				.containsEntry("customer_uuid", CUSTOMER_UUID)
				.containsEntry("type", "MONEY_DEPOSITED")
				.containsEntry("message", "Счёт *0001: пополнение на 500. Доступно 25\u00A0500");
	}

	@Test
	void storesEventPublishedByTransfer() {
		stubTrigger.trigger("money_sent_event");

		assertThat(awaitNotificationOf(MONEY_SENT_EVENT_UUID))
				.containsEntry("customer_uuid", CUSTOMER_UUID)
				.containsEntry("type", "MONEY_SENT");
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
