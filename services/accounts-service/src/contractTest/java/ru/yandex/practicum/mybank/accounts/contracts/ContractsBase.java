package ru.yandex.practicum.mybank.accounts.contracts;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ru.yandex.practicum.mybank.accounts.controller.ApiExceptionHandler;
import ru.yandex.practicum.mybank.accounts.controller.CustomerController;
import ru.yandex.practicum.mybank.accounts.controller.TransactionController;
import ru.yandex.practicum.mybank.accounts.domain.OperationDirection;
import ru.yandex.practicum.mybank.accounts.domain.TransactionType;
import ru.yandex.practicum.mybank.accounts.service.CustomerAccountNotFoundException;
import ru.yandex.practicum.mybank.accounts.service.CustomerService;
import ru.yandex.practicum.mybank.accounts.service.InsufficientFundsException;
import ru.yandex.practicum.mybank.accounts.service.TransactionConflictException;
import ru.yandex.practicum.mybank.accounts.service.TransactionsService;
import ru.yandex.practicum.mybank.accounts.service.dto.CustomerAccountDto;
import ru.yandex.practicum.mybank.accounts.service.dto.CustomerDto;
import ru.yandex.practicum.mybank.accounts.service.dto.OperationDto;
import ru.yandex.practicum.mybank.accounts.service.dto.TransactionDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class ContractsBase {

	private static final UUID CONFLICTING_TRANSACTION_UUID = UUID.fromString("00000000-0000-0000-0000-000000000409");
	private static final UUID UNKNOWN_CUSTOMER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000404");

	private static final String USER1_NUMBER = "40817810000000000001";
	private static final UUID USER1_ACCOUNT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID USER1_CUSTOMER_UUID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
	private static final String USER2_NUMBER = "40817810000000000002";
	private static final UUID USER2_ACCOUNT_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID USER2_CUSTOMER_UUID = UUID.fromString("aaaaaaaa-2222-2222-2222-222222222222");

	@BeforeEach
	void setUp() {
		TransactionsService transactionsService = mock(TransactionsService.class);
		when(transactionsService.deposit(any(), any(), anyLong())).thenAnswer(invocation ->
				new TransactionDto(invocation.getArgument(0), TransactionType.DEPOSIT,
						List.of(new OperationDto(OperationDirection.DEPOSIT, null, null, null,
								USER1_NUMBER, USER1_ACCOUNT_UUID, USER1_CUSTOMER_UUID,
								invocation.getArgument(2), 25500))));
		when(transactionsService.withdraw(any(), any(), anyLong())).thenAnswer(invocation ->
				new TransactionDto(invocation.getArgument(0), TransactionType.WITHDRAW,
						List.of(new OperationDto(OperationDirection.WITHDRAW,
								USER1_NUMBER, USER1_ACCOUNT_UUID, USER1_CUSTOMER_UUID,
								null, null, null,
								invocation.getArgument(2), 24500))));
		when(transactionsService.transfer(any(), any(), any(), anyLong())).thenAnswer(invocation ->
				new TransactionDto(invocation.getArgument(0), TransactionType.TRANSFER,
						List.of(new OperationDto(OperationDirection.WITHDRAW,
										USER1_NUMBER, USER1_ACCOUNT_UUID, USER1_CUSTOMER_UUID,
										USER2_NUMBER, USER2_ACCOUNT_UUID, USER2_CUSTOMER_UUID,
										invocation.getArgument(3), 24500),
								new OperationDto(OperationDirection.DEPOSIT,
										USER1_NUMBER, USER1_ACCOUNT_UUID, USER1_CUSTOMER_UUID,
										USER2_NUMBER, USER2_ACCOUNT_UUID, USER2_CUSTOMER_UUID,
										invocation.getArgument(3), 5500))));
		doThrow(new TransactionConflictException(CONFLICTING_TRANSACTION_UUID))
				.when(transactionsService).deposit(eq(CONFLICTING_TRANSACTION_UUID), any(), anyLong());
		doThrow(new InsufficientFundsException("user1", 1_000_000_000_000L, 25000))
				.when(transactionsService).withdraw(any(), any(), eq(1_000_000_000_000L));

		CustomerService customerService = mock(CustomerService.class);

		when(customerService.getCustomerAccount("user1")).thenReturn(new CustomerAccountDto("user1",
				"Иванов Иван", LocalDate.of(1990, 1, 15), USER1_NUMBER, 25000));
		when(customerService.updateProfile(eq("user1"), any(), any())).thenAnswer(invocation ->
				new CustomerAccountDto("user1", invocation.getArgument(1), invocation.getArgument(2),
						USER1_NUMBER, 25000));
		when(customerService.findOthers("user1")).thenReturn(List.of(
				new CustomerDto(USER2_CUSTOMER_UUID, "user2", "Петров Пётр")));
		when(customerService.getCustomer(any())).thenAnswer(invocation ->
				new CustomerDto(invocation.getArgument(0), "user1", "Иванов Иван"));
		doThrow(new CustomerAccountNotFoundException(UNKNOWN_CUSTOMER_UUID))
				.when(customerService).getCustomer(UNKNOWN_CUSTOMER_UUID);

		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("token")
				.header("alg", "none")
				.claim("preferred_username", "user1")
				.build()));

		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		RestAssuredMockMvc.standaloneSetup(MockMvcBuilders
				.standaloneSetup(
						new TransactionController(transactionsService),
						new CustomerController(customerService))
				.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
				.setControllerAdvice(new ApiExceptionHandler())
				.setValidator(validator));
	}
}
