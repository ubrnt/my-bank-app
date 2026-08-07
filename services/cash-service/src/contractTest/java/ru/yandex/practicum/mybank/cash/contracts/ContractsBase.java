package ru.yandex.practicum.mybank.cash.contracts;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ru.yandex.practicum.mybank.cash.client.TransactionRejectedException;
import ru.yandex.practicum.mybank.cash.controller.ApiExceptionHandler;
import ru.yandex.practicum.mybank.cash.controller.CashController;
import ru.yandex.practicum.mybank.cash.domain.CashOperationStatus;
import ru.yandex.practicum.mybank.cash.domain.CashOperationType;
import ru.yandex.practicum.mybank.cash.service.CashService;
import ru.yandex.practicum.mybank.cash.service.dto.CashOperationDto;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class ContractsBase {

	private static final UUID OPERATION_UUID = UUID.fromString("cccccccc-1111-1111-1111-111111111111");
	private static final long BEYOND_BALANCE = 1_000_000_000_000L;

	@BeforeEach
	void setUp() {
		CashService cashService = mock(CashService.class);

		when(cashService.deposit(any(), anyLong())).thenAnswer(invocation ->
				new CashOperationDto(OPERATION_UUID, CashOperationType.DEPOSIT,
						invocation.getArgument(1), CashOperationStatus.COMPLETED));
		when(cashService.withdraw(any(), anyLong())).thenAnswer(invocation ->
				new CashOperationDto(OPERATION_UUID, CashOperationType.WITHDRAW,
						invocation.getArgument(1), CashOperationStatus.COMPLETED));
		doThrow(new TransactionRejectedException("insufficient_funds", "Not enough money on the account"))
				.when(cashService).withdraw(any(), eq(BEYOND_BALANCE));

		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("token")
				.header("alg", "none")
				.claim("preferred_username", "user1")
				.build()));

		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		RestAssuredMockMvc.standaloneSetup(MockMvcBuilders
				.standaloneSetup(new CashController(cashService))
				.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
				.setControllerAdvice(new ApiExceptionHandler())
				.setValidator(validator));
	}
}
