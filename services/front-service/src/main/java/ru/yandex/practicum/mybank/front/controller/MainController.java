package ru.yandex.practicum.mybank.front.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.yandex.practicum.mybank.front.client.GatewayClient;
import ru.yandex.practicum.mybank.front.client.GatewayException;
import ru.yandex.practicum.mybank.front.client.dto.CashRequest;
import ru.yandex.practicum.mybank.front.client.dto.CustomerResponse;
import ru.yandex.practicum.mybank.front.client.dto.ErrorResponse;
import ru.yandex.practicum.mybank.front.client.dto.TransferRequest;
import ru.yandex.practicum.mybank.front.client.dto.UpdateProfileRequest;
import ru.yandex.practicum.mybank.front.controller.dto.AccountDto;
import ru.yandex.practicum.mybank.front.controller.dto.CashAction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контроллер main.html.
 *
 * Используемая модель для main.html:
 *      model.addAttribute("name", name);
 *      model.addAttribute("birthdate", birthdate.format(DateTimeFormatter.ISO_DATE));
 *      model.addAttribute("sum", sum);
 *      model.addAttribute("accounts", accounts);
 *      model.addAttribute("errors", errors);
 *      model.addAttribute("info", info);
 *
 * Поля модели:
 *      name - Фамилия Имя текущего пользователя, String (обязательное)
 *      birthdate - дата рождения текущего пользователя, String в формате 'YYYY-MM-DD' (обязательное)
 *      sum - сумма на счету текущего пользователя, Integer (обязательное)
 *      accounts - список аккаунтов, которым можно перевести деньги, List<AccountDto> (обязательное)
 *      errors - список ошибок после выполнения действий, List<String> (не обязательное)
 *      info - строка успешности после выполнения действия, String (не обязательное)
 *
 * С примерами использования можно ознакомиться в тестовом классе заглушке AccountStub
 */
@Controller
public class MainController {

	private static final String IDEMPOTENCY_KEY_CONFLICT = "idempotency_key_conflict";
	private static final String CASH_PATH = "/cash";

	private final GatewayClient gatewayClient;

	private final MessageRenderer messages;

	public MainController(GatewayClient gatewayClient, MessageRenderer messages) {
		this.gatewayClient = gatewayClient;
		this.messages = messages;
	}

	@GetMapping
	public String index() {
		return "redirect:/account";
	}

	@GetMapping("/account")
	public String getAccount(Model model) {
		fillModel(model, List.of(), null, newKeys());

		return "main";
	}

	@PostMapping("/account")
	public String editAccount(
			Model model,
			@RequestParam("name") String name,
			@RequestParam("birthdate") LocalDate birthdate
	) {
		gatewayClient.updateCustomer(new UpdateProfileRequest(name, birthdate));
		fillModel(model, List.of(), messages.infoMessage("info.profile_updated"), newKeys());

		return "main";
	}

	@PostMapping("/cash")
	public String editCash(
			Model model,
			@RequestParam("idempotencyKey") UUID idempotencyKey,
			@RequestParam("value") long value,
			@RequestParam("action") CashAction action
	) {
		String info = action == CashAction.PUT ? deposit(idempotencyKey, value) : withdraw(idempotencyKey, value);
		fillModel(model, List.of(), info, newKeys());

		return "main";
	}

	@PostMapping("/transfer")
	public String transfer(
			Model model,
			@RequestParam("idempotencyKey") UUID idempotencyKey,
			@RequestParam("value") long value,
			@RequestParam("login") String login
	) {
		gatewayClient.transfer(idempotencyKey, new TransferRequest(login, value));

		List<AccountDto> accounts = fillModel(model, List.of(), null, newKeys());
		model.addAttribute("info", messages.infoMessage("info.transferred", value, nameByLogin(accounts, login)));

		return "main";
	}

	@ExceptionHandler(GatewayException.class)
	public String handleGatewayFailure(GatewayException exception, HttpServletRequest request, Model model) {
		fillModel(model, messages.errorMessages(exception), null, keysAfterFailure(exception, request));

		return "main";
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public String handleBadParameter(MethodArgumentTypeMismatchException exception, HttpServletRequest request,
			Model model) {
		String errorKey = errorKey(exception, request);

		fillModel(model, List.of(messages.errorMessage(errorKey)), null, keysForRetry(request));

		return "main";
	}

	private String errorKey(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
		return switch (exception.getName()) {
			case "value" -> amountErrorKey(String.valueOf(exception.getValue()), request);
			case "birthdate" -> "error.field.birthdate";
			default -> "error.unknown";
		};
	}

	private String amountErrorKey(String submitted, HttpServletRequest request) {
		if (!submitted.matches("\\d+")) {
			return "error.field.amount";
		}

		return CashAction.PUT.name().equals(request.getParameter("action"))
				? "error.balance_limit_exceeded"
				: "error.insufficient_funds";
	}

	private String deposit(UUID idempotencyKey, long value) {
		gatewayClient.deposit(idempotencyKey, new CashRequest(value));

		return messages.infoMessage("info.deposited", value);
	}

	private String withdraw(UUID idempotencyKey, long value) {
		gatewayClient.withdraw(idempotencyKey, new CashRequest(value));

		return messages.infoMessage("info.withdrawn", value);
	}

	private String nameByLogin(List<AccountDto> accounts, String login) {
		return accounts.stream()
				.filter(account -> account.login().equals(login))
				.map(AccountDto::name)
				.findFirst()
				.orElse(login);
	}

	private IdempotencyKeys keysAfterFailure(GatewayException exception, HttpServletRequest request) {
		ErrorResponse response = exception.getResponse();

		if (response != null && IDEMPOTENCY_KEY_CONFLICT.equals(response.code())) {
			return newKeys();
		}

		return keysForRetry(request);
	}

	private IdempotencyKeys keysForRetry(HttpServletRequest request) {
		UUID submitted = submittedIdempotencyKey(request).orElseGet(UUID::randomUUID);

		return CASH_PATH.equals(request.getRequestURI())
				? new IdempotencyKeys(submitted, UUID.randomUUID())
				: new IdempotencyKeys(UUID.randomUUID(), submitted);
	}

	private IdempotencyKeys newKeys() {
		return new IdempotencyKeys(UUID.randomUUID(), UUID.randomUUID());
	}

	private Optional<UUID> submittedIdempotencyKey(HttpServletRequest request) {
		String submitted = request.getParameter("idempotencyKey");

		if (submitted == null) {
			return Optional.empty();
		}

		try {
			return Optional.of(UUID.fromString(submitted));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private List<AccountDto> fillModel(Model model, List<String> errors, String info, IdempotencyKeys keys) {
		List<String> allErrors = new ArrayList<>(errors);
		List<AccountDto> accounts = List.of();

		try {
			CustomerResponse customer = gatewayClient.getCustomer();

			accounts = gatewayClient.getOtherCustomers().stream()
					.map(other -> new AccountDto(other.login(), other.name()))
					.toList();

			model.addAttribute("name", customer.name());
			model.addAttribute("birthdate", customer.birthdate().format(DateTimeFormatter.ISO_DATE));
			model.addAttribute("sum", customer.balance());
			model.addAttribute("accounts", accounts);
		} catch (GatewayException exception) {
			if (allErrors.isEmpty()) {
				allErrors.addAll(messages.errorMessages(exception));
			}
		}

		model.addAttribute("errors", allErrors.isEmpty() ? null : allErrors);
		model.addAttribute("info", info);
		model.addAttribute("cashIdempotencyKey", keys.cash());
		model.addAttribute("transferIdempotencyKey", keys.transfer());

		return accounts;
	}

	private record IdempotencyKeys(
			UUID cash,
			UUID transfer
	) {
	}
}
