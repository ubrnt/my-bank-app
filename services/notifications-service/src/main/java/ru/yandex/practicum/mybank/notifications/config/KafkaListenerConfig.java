package ru.yandex.practicum.mybank.notifications.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListenerConfigurer;
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ru.yandex.practicum.mybank.notifications.client.UnknownRecipientException;
import ru.yandex.practicum.mybank.notifications.service.InvalidEventException;

@Configuration
public class KafkaListenerConfig implements KafkaListenerConfigurer {

	private final LocalValidatorFactoryBean validator;

	public KafkaListenerConfig(LocalValidatorFactoryBean validator) {
		this.validator = validator;
	}

	@Override
	public void configureKafkaListeners(@NonNull KafkaListenerEndpointRegistrar registrar) {
		registrar.setValidator(validator);
	}

	@Bean
	public CommonErrorHandler notificationsErrorHandler() {
		DefaultErrorHandler errorHandler = new DefaultErrorHandler(new ExponentialBackOff());

		errorHandler.addNotRetryableExceptions(InvalidEventException.class, UnknownRecipientException.class);

		return errorHandler;
	}
}
