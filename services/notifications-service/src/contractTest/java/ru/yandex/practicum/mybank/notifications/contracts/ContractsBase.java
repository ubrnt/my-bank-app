package ru.yandex.practicum.mybank.notifications.contracts;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ru.yandex.practicum.mybank.notifications.controller.ApiExceptionHandler;
import ru.yandex.practicum.mybank.notifications.controller.NotificationsController;
import ru.yandex.practicum.mybank.notifications.domain.EventType;
import ru.yandex.practicum.mybank.notifications.service.InvalidEventException;
import ru.yandex.practicum.mybank.notifications.service.NotificationsService;
import tools.jackson.databind.JsonNode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public abstract class ContractsBase {

	@BeforeEach
	void setUp() {
		NotificationsService notificationsService = mock(NotificationsService.class);
		doThrow(new InvalidEventException("Event payload lacks field 'operation'"))
				.when(notificationsService)
				.receive(any(), argThat(type -> type != EventType.PROFILE_UPDATED), any(),
						argThat((JsonNode payload) -> !payload.has("operation")));

		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		RestAssuredMockMvc.standaloneSetup(MockMvcBuilders
				.standaloneSetup(new NotificationsController(notificationsService))
				.setControllerAdvice(new ApiExceptionHandler())
				.setValidator(validator));
	}
}
