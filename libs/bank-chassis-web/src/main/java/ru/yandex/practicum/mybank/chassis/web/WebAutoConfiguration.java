package ru.yandex.practicum.mybank.chassis.web;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public RequestLoggingFilter requestLoggingFilter(
			@Value("${management.endpoints.web.base-path:/actuator}") String actuatorBasePath) {
		return new RequestLoggingFilter(actuatorBasePath);
	}

	@Bean
	public ObservationPredicate actuatorObservationPredicate(
			@Value("${management.endpoints.web.base-path:/actuator}") String actuatorBasePath) {
		return (name, context) -> !(context instanceof ServerRequestObservationContext serverContext
				&& serverContext.getCarrier().getRequestURI().startsWith(actuatorBasePath));
	}
}
