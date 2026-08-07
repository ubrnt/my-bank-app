package ru.yandex.practicum.mybank.front.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.mybank.front.client.GatewayClient;
import ru.yandex.practicum.mybank.front.client.GatewayException;
import ru.yandex.practicum.mybank.front.client.dto.CashRequest;
import ru.yandex.practicum.mybank.front.client.dto.CustomerResponse;
import ru.yandex.practicum.mybank.front.client.dto.TransferRequest;
import ru.yandex.practicum.mybank.front.client.dto.UpdateProfileRequest;
import ru.yandex.practicum.mybank.front.controller.dto.AccountDto;
import ru.yandex.practicum.mybank.front.controller.dto.CashAction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
		fillModel(model, List.of(), null);

		return "main";
	}

	@PostMapping("/account")
	public String editAccount(
			Model model,
			@RequestParam("name") String name,
			@RequestParam("birthdate") LocalDate birthdate
	) {
		gatewayClient.updateCustomer(new UpdateProfileRequest(name, birthdate));
		fillModel(model, List.of(), messages.infoMessage("info.profile_updated"));

		return "main";
	}

	@PostMapping("/cash")
	public String editCash(
			Model model,
			@RequestParam("value") long value,
			@RequestParam("action") CashAction action
	) {
		String info = action == CashAction.PUT ? deposit(value) : withdraw(value);
		fillModel(model, List.of(), info);

		return "main";
	}

	@PostMapping("/transfer")
	public String transfer(
			Model model,
			@RequestParam("value") long value,
			@RequestParam("login") String login
	) {
		gatewayClient.transfer(new TransferRequest(login, value));
		fillModel(model, List.of(), messages.infoMessage("info.transferred", value, login));

		return "main";
	}

	@ExceptionHandler(GatewayException.class)
	public String handleGatewayFailure(GatewayException exception, Model model) {
		fillModel(model, messages.errorMessages(exception), null);

		return "main";
	}

	private String deposit(long value) {
		gatewayClient.deposit(new CashRequest(value));

		return messages.infoMessage("info.deposited", value);
	}

	private String withdraw(long value) {
		gatewayClient.withdraw(new CashRequest(value));

		return messages.infoMessage("info.withdrawn", value);
	}

	private void fillModel(Model model, List<String> errors, String info) {
		List<String> allErrors = new ArrayList<>(errors);

		try {
			CustomerResponse customer = gatewayClient.getCustomer();

			List<AccountDto> accounts = gatewayClient.getOtherCustomers().stream()
					.map(other -> new AccountDto(other.login(), other.name()))
					.toList();

			model.addAttribute("name", customer.name());
			model.addAttribute("birthdate", customer.birthdate().format(DateTimeFormatter.ISO_DATE));
			model.addAttribute("sum", customer.balance());
			model.addAttribute("accounts", accounts);
		} catch (GatewayException exception) {
			allErrors.addAll(messages.errorMessages(exception));
		}

		model.addAttribute("errors", allErrors.isEmpty() ? null : allErrors);
		model.addAttribute("info", info);
	}
}
