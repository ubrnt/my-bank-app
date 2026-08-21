package ru.yandex.practicum.mybank.chassis.observability;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@AutoConfiguration
@ConditionalOnClass(ServerRequestObservationContext.class)
public class ObservabilityAutoConfiguration {

	@Bean
	public ObservationPredicate actuatorObservationPredicate(
			@Value("${management.endpoints.web.base-path:/actuator}") String actuatorBasePath) {
		return (name, context) -> !(context instanceof ServerRequestObservationContext serverContext
				&& serverContext.getCarrier().getRequestURI().startsWith(actuatorBasePath));
	}
}
