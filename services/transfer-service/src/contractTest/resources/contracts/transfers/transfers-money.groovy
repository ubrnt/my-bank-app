package contracts.transfers

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "transfers money from the logged in customer to another one"
	priority 2
	request {
		method POST()
		url "/api/transfers"
		headers {
			contentType(applicationJson())
		}
		body(
				toLogin: "user2",
				amount: 500
		)
	}
	response {
		status OK()
		headers {
			contentType(applicationJson())
		}
		body(
				uuid: $(anyUuid()),
				amount: 500,
				status: "completed"
		)
	}
}
