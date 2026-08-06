package ru.yandex.practicum.mybank.cash.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.mybank.cash.dto.CashRequest;
import ru.yandex.practicum.mybank.cash.service.CashService;
import ru.yandex.practicum.mybank.cash.service.dto.CashOperationDto;

@RestController
@RequestMapping("/api/cash")
public class CashController {

	private final CashService cashService;

	public CashController(CashService cashService) {
		this.cashService = cashService;
	}

	@PostMapping("/deposit")
	public CashOperationDto deposit(@Valid @RequestBody CashRequest request, @AuthenticationPrincipal Jwt jwt) {
		return cashService.deposit(login(jwt), request.amount());
	}

	@PostMapping("/withdraw")
	public CashOperationDto withdraw(@Valid @RequestBody CashRequest request, @AuthenticationPrincipal Jwt jwt) {
		return cashService.withdraw(login(jwt), request.amount());
	}

	private String login(Jwt jwt) {
		return jwt.getClaimAsString("preferred_username");
	}
}
