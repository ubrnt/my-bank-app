package ru.yandex.practicum.mybank.contract.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

@Configuration
public class KafkaContractMessagingConfiguration {

	@Bean(destroyMethod = "close")
	public KafkaMessageVerifier kafkaMessageVerifier(
			@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
		return new KafkaMessageVerifier(bootstrapServers);
	}

	@Bean
	public ContractVerifierMessaging<Message<?>> contractVerifierMessaging(KafkaMessageVerifier verifier) {
		return new KafkaContractVerifierMessaging(verifier);
	}
}
