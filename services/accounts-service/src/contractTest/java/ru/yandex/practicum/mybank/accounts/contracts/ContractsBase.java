package ru.yandex.practicum.mybank.accounts.contracts;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
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
import ru.yandex.practicum.mybank.accounts.service.dto.CustomerDto;
import ru.yandex.practicum.mybank.accounts.service.dto.OperationDto;
import ru.yandex.practicum.mybank.accounts.service.dto.TransactionDto;

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
						new OperationDto(OperationDirection.DEPOSIT, null, null, null,
								USER1_NUMBER, USER1_ACCOUNT_UUID, USER1_CUSTOMER_UUID,
								invocation.getArgument(2), 25500)));
		when(transactionsService.withdraw(any(), any(), anyLong())).thenAnswer(invocation ->
				new TransactionDto(invocation.getArgument(0), TransactionType.WITHDRAW,
						new OperationDto(OperationDirection.WITHDRAW,
								USER1_NUMBER, USER1_ACCOUNT_UUID, USER1_CUSTOMER_UUID,
								null, null, null,
								invocation.getArgument(2), 24500)));
		when(transactionsService.transfer(any(), any(), any(), anyLong())).thenAnswer(invocation ->
				new TransactionDto(invocation.getArgument(0), TransactionType.TRANSFER,
						new OperationDto(OperationDirection.WITHDRAW,
								USER1_NUMBER, USER1_ACCOUNT_UUID, USER1_CUSTOMER_UUID,
								USER2_NUMBER, USER2_ACCOUNT_UUID, USER2_CUSTOMER_UUID,
								invocation.getArgument(3), 24500)));
		doThrow(new TransactionConflictException(CONFLICTING_TRANSACTION_UUID))
				.when(transactionsService).deposit(eq(CONFLICTING_TRANSACTION_UUID), any(), anyLong());
		doThrow(new InsufficientFundsException("user1", 1_000_000_000_000L, 25000))
				.when(transactionsService).withdraw(any(), any(), eq(1_000_000_000_000L));

		CustomerService customerService = mock(CustomerService.class);
		when(customerService.getCustomer(any())).thenAnswer(invocation ->
				new CustomerDto(invocation.getArgument(0), "user1", "user1_first_name user1_last_name"));
		doThrow(new CustomerAccountNotFoundException(UNKNOWN_CUSTOMER_UUID))
				.when(customerService).getCustomer(UNKNOWN_CUSTOMER_UUID);

		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		RestAssuredMockMvc.standaloneSetup(MockMvcBuilders
				.standaloneSetup(
						new TransactionController(transactionsService),
						new CustomerController(customerService))
				.setControllerAdvice(new ApiExceptionHandler())
				.setValidator(validator));
	}
}
