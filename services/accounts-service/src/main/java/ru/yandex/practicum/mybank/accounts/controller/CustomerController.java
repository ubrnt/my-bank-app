package ru.yandex.practicum.mybank.accounts.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.mybank.chassis.web.JwtLogin;
import ru.yandex.practicum.mybank.accounts.dto.CustomerAccountResponse;
import ru.yandex.practicum.mybank.accounts.dto.CustomerResponse;
import ru.yandex.practicum.mybank.accounts.dto.UpdateProfileRequest;
import ru.yandex.practicum.mybank.accounts.service.CustomerService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

	private final CustomerService customerService;

	public CustomerController(CustomerService customerService) {
		this.customerService = customerService;
	}

	@GetMapping("/me")
	public CustomerAccountResponse me(@AuthenticationPrincipal Jwt jwt) {
		return CustomerAccountResponse.of(customerService.getCustomerAccount(JwtLogin.of(jwt)));
	}

	@PutMapping("/me")
	public CustomerAccountResponse updateMe(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody UpdateProfileRequest request) {
		return CustomerAccountResponse.of(
				customerService.updateProfile(JwtLogin.of(jwt), request.name(), request.birthdate()));
	}

	@GetMapping("/others")
	public List<CustomerResponse> others(@AuthenticationPrincipal Jwt jwt) {
		return customerService.findOthers(JwtLogin.of(jwt)).stream()
				.map(CustomerResponse::of)
				.toList();
	}

	@GetMapping("/{uuid}")
	public CustomerResponse customer(@PathVariable UUID uuid) {
		return CustomerResponse.of(customerService.getCustomer(uuid));
	}

}
