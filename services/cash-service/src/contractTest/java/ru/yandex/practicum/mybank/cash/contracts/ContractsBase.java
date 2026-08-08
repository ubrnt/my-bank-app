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
import ru.yandex.practicum.mybank.cash.service.DuplicateRequestException;
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
	private static final UUID IN_PROGRESS_KEY = UUID.fromString("dddddddd-1111-1111-1111-111111111111");
	private static final long BEYOND_BALANCE = 1_000_000_000_000L;

	@BeforeEach
	void setUp() {
		CashService cashService = mock(CashService.class);

		when(cashService.deposit(any(), any(), anyLong())).thenAnswer(invocation ->
				new CashOperationDto(OPERATION_UUID, CashOperationType.DEPOSIT,
						invocation.getArgument(2), CashOperationStatus.COMPLETED));
		when(cashService.withdraw(any(), any(), anyLong())).thenAnswer(invocation ->
				new CashOperationDto(OPERATION_UUID, CashOperationType.WITHDRAW,
						invocation.getArgument(2), CashOperationStatus.COMPLETED));
		doThrow(new TransactionRejectedException("insufficient_funds", "Not enough money on the account"))
				.when(cashService).withdraw(any(), any(), eq(BEYOND_BALANCE));
		doThrow(new DuplicateRequestException(IN_PROGRESS_KEY))
				.when(cashService).deposit(eq(IN_PROGRESS_KEY), any(), anyLong());

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
