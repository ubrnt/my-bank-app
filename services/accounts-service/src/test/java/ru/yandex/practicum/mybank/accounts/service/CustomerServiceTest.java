package ru.yandex.practicum.mybank.accounts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.practicum.mybank.accounts.domain.Account;
import ru.yandex.practicum.mybank.accounts.domain.Customer;
import ru.yandex.practicum.mybank.accounts.domain.CustomerAccount;
import ru.yandex.practicum.mybank.accounts.repository.AccountRepository;
import ru.yandex.practicum.mybank.accounts.repository.CustomerRepository;
import ru.yandex.practicum.mybank.notifications.outbox.NotificationsOutboxService;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerServiceTest {

	private static final String NAME = "Иванов Иван";
	private static final LocalDate BIRTHDATE = LocalDate.of(1990, 1, 15);

	private final AccountRepository accountRepository = mock(AccountRepository.class);
	private final NotificationsOutboxService notificationsOutboxService = mock(NotificationsOutboxService.class);

	private CustomerService customerService;
	private Customer customer;

	@BeforeEach
	void setUp() {
		customer = new Customer("user1", NAME, BIRTHDATE);
		ReflectionTestUtils.setField(customer, "id", 1L);

		when(accountRepository.findCustomerAccount("user1")).thenReturn(Optional.of(
				new CustomerAccount(customer, new Account("40817810000000000001", customer))));

		customerService = new CustomerService(mock(CustomerRepository.class), accountRepository,
				notificationsOutboxService);
	}

	@Test
	void emitsAnEventWhenTheNameChanges() {
		customerService.updateProfile("user1", "Иванов Игорь", BIRTHDATE);

		assertThat(customer.getName()).isEqualTo("Иванов Игорь");
		verify(notificationsOutboxService).save(anyString(), anyString(), anyLong(), any(), any());
	}

	@Test
	void emitsAnEventWhenTheBirthdateChanges() {
		customerService.updateProfile("user1", NAME, LocalDate.of(1991, 2, 16));

		assertThat(customer.getBirthdate()).isEqualTo(LocalDate.of(1991, 2, 16));
		verify(notificationsOutboxService).save(anyString(), anyString(), anyLong(), any(), any());
	}

	@Test
	void emitsNothingWhenBothValuesStayTheSame() {
		customerService.updateProfile("user1", NAME, BIRTHDATE);

		verify(notificationsOutboxService, never()).save(anyString(), anyString(), anyLong(), any(), any());
	}
}
