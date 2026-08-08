package ru.yandex.practicum.mybank.transfer.contracts;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ru.yandex.practicum.mybank.transfer.client.TransactionRejectedException;
import ru.yandex.practicum.mybank.transfer.controller.ApiExceptionHandler;
import ru.yandex.practicum.mybank.transfer.controller.TransferController;
import ru.yandex.practicum.mybank.transfer.domain.TransferOperationStatus;
import ru.yandex.practicum.mybank.transfer.service.DuplicateRequestException;
import ru.yandex.practicum.mybank.transfer.service.TransferService;
import ru.yandex.practicum.mybank.transfer.service.dto.TransferOperationDto;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class ContractsBase {

	private static final UUID OPERATION_UUID = UUID.fromString("dddddddd-1111-1111-1111-111111111111");
	private static final UUID IN_PROGRESS_KEY = UUID.fromString("eeeeeeee-1111-1111-1111-111111111111");
	private static final long BEYOND_BALANCE = 1_000_000_000_000L;

	@BeforeEach
	void setUp() {
		TransferService transferService = mock(TransferService.class);

		when(transferService.transfer(any(), any(), any(), anyLong())).thenAnswer(invocation ->
				new TransferOperationDto(OPERATION_UUID, invocation.getArgument(3),
						TransferOperationStatus.COMPLETED));
		doThrow(new TransactionRejectedException("insufficient_funds", "Not enough money on the account"))
				.when(transferService).transfer(any(), any(), any(), eq(BEYOND_BALANCE));
		doThrow(new TransactionRejectedException("same_account", "Cannot transfer to the same account"))
				.when(transferService).transfer(any(), any(), eq("user1"), anyLong());
		doThrow(new DuplicateRequestException(IN_PROGRESS_KEY))
				.when(transferService).transfer(eq(IN_PROGRESS_KEY), any(), any(), anyLong());

		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("token")
				.header("alg", "none")
				.claim("preferred_username", "user1")
				.build()));

		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		RestAssuredMockMvc.standaloneSetup(MockMvcBuilders
				.standaloneSetup(new TransferController(transferService))
				.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
				.setControllerAdvice(new ApiExceptionHandler())
				.setValidator(validator));
	}
}
