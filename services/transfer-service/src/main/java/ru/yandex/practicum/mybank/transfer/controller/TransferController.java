package ru.yandex.practicum.mybank.transfer.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.mybank.transfer.dto.TransferRequest;
import ru.yandex.practicum.mybank.transfer.service.TransferService;
import ru.yandex.practicum.mybank.transfer.dto.TransferOperationResponse;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

	private final TransferService transferService;

	public TransferController(TransferService transferService) {
		this.transferService = transferService;
	}

	@PostMapping
	public TransferOperationResponse transfer(@Valid @RequestBody TransferRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return TransferOperationResponse.of(transferService.transfer(login(jwt), request.toLogin(), request.amount()));
	}

	private String login(Jwt jwt) {
		return jwt.getClaimAsString("preferred_username");
	}
}
