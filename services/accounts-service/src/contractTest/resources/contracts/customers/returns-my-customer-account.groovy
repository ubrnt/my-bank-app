package contracts.customers

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "returns the card of the logged in customer with the account balance"
	request {
		method GET()
		url "/api/customers/me"
	}
	response {
		status OK()
		headers {
			contentType(applicationJson())
		}
		body(
				login: "user1",
				name: "Иванов Иван",
				birthdate: "1990-01-15",
				number: "40817810000000000001",
				balance: 25000
		)
	}
}
