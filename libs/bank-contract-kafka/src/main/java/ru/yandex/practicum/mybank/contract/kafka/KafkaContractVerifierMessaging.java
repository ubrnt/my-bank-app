package ru.yandex.practicum.mybank.contract.kafka;

import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessage;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessaging;
import org.springframework.messaging.Message;

public class KafkaContractVerifierMessaging extends ContractVerifierMessaging<Message<?>> {

	public KafkaContractVerifierMessaging(KafkaMessageVerifier verifier) {
		super(verifier, verifier);
	}

	@Override
	protected ContractVerifierMessage convert(Message<?> message) {
		if (message == null) {
			return null;
		}

		return new ContractVerifierMessage(message.getPayload(), message.getHeaders());
	}
}
