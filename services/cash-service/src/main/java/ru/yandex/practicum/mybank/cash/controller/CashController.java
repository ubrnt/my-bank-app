package ru.yandex.practicum.mybank.cash.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.mybank.chassis.web.JwtLogin;
import ru.yandex.practicum.mybank.cash.dto.CashOperationResponse;
import ru.yandex.practicum.mybank.cash.dto.CashRequest;
import ru.yandex.practicum.mybank.cash.service.CashService;

import java.util.UUID;

@RestController
@RequestMapping("/api/cash")
public class CashController {

	private final CashService cashService;

	public CashController(CashService cashService) {
		this.cashService = cashService;
	}

	@PostMapping("/deposit")
	public CashOperationResponse deposit(@RequestHeader("Idempotency-Key") UUID idempotencyKey,
			@Valid @RequestBody CashRequest request, @AuthenticationPrincipal Jwt jwt) {
		return CashOperationResponse.of(cashService.deposit(idempotencyKey, JwtLogin.of(jwt), request.amount()));
	}

	@PostMapping("/withdraw")
	public CashOperationResponse withdraw(@RequestHeader("Idempotency-Key") UUID idempotencyKey,
			@Valid @RequestBody CashRequest request, @AuthenticationPrincipal Jwt jwt) {
		return CashOperationResponse.of(cashService.withdraw(idempotencyKey, JwtLogin.of(jwt), request.amount()));
	}

}
