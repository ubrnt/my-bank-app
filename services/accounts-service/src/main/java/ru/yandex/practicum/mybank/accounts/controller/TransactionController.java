package ru.yandex.practicum.mybank.accounts.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.mybank.accounts.dto.DepositRequest;
import ru.yandex.practicum.mybank.accounts.dto.TransactionResponse;
import ru.yandex.practicum.mybank.accounts.dto.TransferRequest;
import ru.yandex.practicum.mybank.accounts.dto.WithdrawRequest;
import ru.yandex.practicum.mybank.accounts.service.BalanceService;
import ru.yandex.practicum.mybank.accounts.service.dto.TransactionDto;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

	private final BalanceService balanceService;

	public TransactionController(BalanceService balanceService) {
		this.balanceService = balanceService;
	}

	@PostMapping("/deposit")
	public TransactionResponse deposit(@Valid @RequestBody DepositRequest request) {
		TransactionDto transaction = balanceService.deposit(
				request.transactionId(), request.login(), request.amount());

		return TransactionResponse.of(transaction, request.login());
	}

	@PostMapping("/withdraw")
	public TransactionResponse withdraw(@Valid @RequestBody WithdrawRequest request) {
		TransactionDto transaction = balanceService.withdraw(
				request.transactionId(), request.login(), request.amount());

		return TransactionResponse.of(transaction, request.login());
	}

	@PostMapping("/transfer")
	public TransactionResponse transfer(@Valid @RequestBody TransferRequest request) {
		TransactionDto transaction = balanceService.transfer(
				request.transactionId(), request.fromLogin(),
				request.toLogin(), request.amount());

		return TransactionResponse.of(transaction, request.fromLogin());
	}
}
