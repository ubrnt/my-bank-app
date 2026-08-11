package contracts.cash

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "puts money on the account of the logged in customer"
	priority 2
	request {
		method POST()
		url "/api/cash/deposit"
		headers {
			contentType(applicationJson())
			header("Idempotency-Key", $(consumer(regex(uuid())), producer("aaaaaaaa-2222-3333-4444-555555555555")))
		}
		body(
				amount: 1500
		)
	}
	response {
		status OK()
		headers {
			contentType(applicationJson())
		}
		body(
				uuid: $(anyUuid()),
				type: "deposit",
				amount: 1500,
				status: "completed"
		)
	}
}
