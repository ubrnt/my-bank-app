package contracts.customers

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "updates the profile of the logged in customer"
	request {
		method PUT()
		url "/api/customers/me"
		headers {
			contentType(applicationJson())
		}
		body(
				name: "Иванов Иван",
				birthdate: "1990-01-15"
		)
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
