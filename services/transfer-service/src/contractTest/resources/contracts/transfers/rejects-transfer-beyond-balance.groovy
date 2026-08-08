package contracts.transfers

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "rejects a transfer that exceeds the available balance"
	priority 1
	request {
		method POST()
		url "/api/transfers"
		headers {
			contentType(applicationJson())
			header("Idempotency-Key", $(consumer(regex(uuid())), producer("bbbbbbbb-3333-3333-3333-555555555555")))
		}
		body(
				toLogin: "user2",
				amount: 1000000000000
		)
	}
	response {
		status UNPROCESSABLE_ENTITY()
		headers {
			contentType(applicationJson())
		}
		body(
				code: "insufficient_funds",
				message: $(anyNonBlankString())
		)
	}
}
