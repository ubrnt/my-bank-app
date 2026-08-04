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
import ru.yandex.practicum.mybank.accounts.service.TransactionsService;
import ru.yandex.practicum.mybank.accounts.service.dto.TransactionDto;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

	private final TransactionsService transactionsService;

	public TransactionController(TransactionsService transactionsService) {
		this.transactionsService = transactionsService;
	}

	@PostMapping("/deposit")
	public TransactionResponse deposit(@Valid @RequestBody DepositRequest request) {
		TransactionDto transaction = transactionsService.deposit(
				request.transactionId(), request.login(), request.amount());

		return TransactionResponse.of(transaction);
	}

	@PostMapping("/withdraw")
	public TransactionResponse withdraw(@Valid @RequestBody WithdrawRequest request) {
		TransactionDto transaction = transactionsService.withdraw(
				request.transactionId(), request.login(), request.amount());

		return TransactionResponse.of(transaction);
	}

	@PostMapping("/transfer")
	public TransactionResponse transfer(@Valid @RequestBody TransferRequest request) {
		TransactionDto transaction = transactionsService.transfer(
				request.transactionId(), request.fromLogin(),
				request.toLogin(), request.amount());

		return TransactionResponse.of(transaction);
	}
}
