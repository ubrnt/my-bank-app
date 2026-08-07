package contracts.customers

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "returns other customers without their balances"
	request {
		method GET()
		url "/api/customers/others"
	}
	response {
		status OK()
		headers {
			contentType(applicationJson())
		}
		body([
				[
						login: "user2",
						name : "user2_first_name user2_last_name"
				]
		])
	}
}
