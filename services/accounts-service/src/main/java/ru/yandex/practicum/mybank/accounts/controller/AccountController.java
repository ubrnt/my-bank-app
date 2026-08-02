package ru.yandex.practicum.mybank.accounts.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.mybank.accounts.dto.AccountResponse;
import ru.yandex.practicum.mybank.accounts.dto.CustomerSummaryResponse;
import ru.yandex.practicum.mybank.accounts.service.AccountNotFoundException;
import ru.yandex.practicum.mybank.accounts.service.AccountService;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

	private final AccountService accountService;

	public AccountController(AccountService accountService) {
		this.accountService = accountService;
	}

	@GetMapping("/me")
	public AccountResponse me(@AuthenticationPrincipal Jwt jwt) {
		return AccountResponse.of(accountService.findAccountOf(login(jwt)));
	}

	@GetMapping("/others")
	public List<CustomerSummaryResponse> others(@AuthenticationPrincipal Jwt jwt) {
		return accountService.findOtherCustomers(login(jwt)).stream()
				.map(CustomerSummaryResponse::of)
				.toList();
	}

	@ExceptionHandler(AccountNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public void handleNotFound() {
	}

	private String login(Jwt jwt) {
		return jwt.getClaimAsString("preferred_username");
	}
}
