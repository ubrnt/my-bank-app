package ru.yandex.practicum.mybank.transfer.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.mybank.chassis.web.JwtLogin;
import ru.yandex.practicum.mybank.transfer.dto.TransferRequest;
import ru.yandex.practicum.mybank.transfer.service.TransferService;
import ru.yandex.practicum.mybank.transfer.dto.TransferOperationResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

	private final TransferService transferService;

	public TransferController(TransferService transferService) {
		this.transferService = transferService;
	}

	@PostMapping
	public TransferOperationResponse transfer(@RequestHeader("Idempotency-Key") UUID idempotencyKey,
			@Valid @RequestBody TransferRequest request, @AuthenticationPrincipal Jwt jwt) {
		return TransferOperationResponse.of(
				transferService.transfer(idempotencyKey, JwtLogin.of(jwt), request.toLogin(), request.amount()));
	}

}
