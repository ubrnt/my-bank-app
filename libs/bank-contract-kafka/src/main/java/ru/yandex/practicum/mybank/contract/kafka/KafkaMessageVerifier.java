package ru.yandex.practicum.mybank.contract.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.cloud.contract.verifier.converter.YamlContract;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifierReceiver;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifierSender;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class KafkaMessageVerifier implements MessageVerifierSender<Message<?>>, MessageVerifierReceiver<Message<?>>,
		AutoCloseable {

	private static final Duration RECEIVE_TIMEOUT = Duration.ofSeconds(15);
	private static final Duration POLL_INTERVAL = Duration.ofMillis(200);

	private final String bootstrapServers;
	private final Producer<String, byte[]> producer;
	private final Map<String, Consumer<String, byte[]>> consumers = new ConcurrentHashMap<>();

	public KafkaMessageVerifier(String bootstrapServers) {
		this.bootstrapServers = bootstrapServers;
		this.producer = new KafkaProducer<>(Map.of(
				ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
				ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
				ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName()));
	}

	@Override
	public void send(Message<?> message, String destination, YamlContract contract) {
		send(message.getPayload(), message.getHeaders(), destination, contract);
	}

	@Override
	public <T> void send(T payload, Map<String, Object> headers, String destination, YamlContract contract) {
		ProducerRecord<String, byte[]> record = new ProducerRecord<>(destination, payloadBytes(payload));

		if (headers != null) {
			headers.forEach((name, value) ->
					record.headers().add(name, String.valueOf(value).getBytes(StandardCharsets.UTF_8)));
		}

		producer.send(record);
		producer.flush();
	}

	@Override
	public Message<?> receive(String destination, long timeout, TimeUnit unit, YamlContract contract) {
		Consumer<String, byte[]> consumer = consumers.computeIfAbsent(destination, this::consumerSubscribedTo);
		Instant deadline = Instant.now().plusMillis(unit.toMillis(timeout));

		while (Instant.now().isBefore(deadline)) {
			ConsumerRecords<String, byte[]> records = consumer.poll(POLL_INTERVAL);

			for (ConsumerRecord<String, byte[]> record : records) {
				return message(record);
			}
		}

		return null;
	}

	@Override
	public Message<?> receive(String destination, YamlContract contract) {
		return receive(destination, RECEIVE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS, contract);
	}

	@Override
	public void close() {
		consumers.values().forEach(Consumer::close);
		producer.close();
	}

	private Consumer<String, byte[]> consumerSubscribedTo(String destination) {
		Map<String, Object> properties = new HashMap<>();

		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		properties.put(ConsumerConfig.GROUP_ID_CONFIG, "contract-verifier-" + UUID.randomUUID());
		properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());

		Consumer<String, byte[]> consumer = new KafkaConsumer<>(properties);

		consumer.subscribe(List.of(destination));

		return consumer;
	}

	private Message<?> message(ConsumerRecord<String, byte[]> record) {
		Map<String, Object> headers = new HashMap<>();

		for (Header header : record.headers()) {
			headers.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
		}

		return MessageBuilder.withPayload(record.value()).copyHeaders(headers).build();
	}

	private byte[] payloadBytes(Object payload) {
		if (payload instanceof byte[] bytes) {
			return bytes;
		}

		return String.valueOf(payload).getBytes(StandardCharsets.UTF_8);
	}
}
