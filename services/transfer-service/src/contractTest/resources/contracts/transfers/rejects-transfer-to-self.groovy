package contracts.transfers

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "rejects a transfer where the recipient is the sender"
	priority 1
	request {
		method POST()
		url "/api/transfers"
		headers {
			contentType(applicationJson())
			header("Idempotency-Key", $(consumer(regex(uuid())), producer("cccccccc-3333-3333-3333-555555555555")))
		}
		body(
				toLogin: "user1",
				amount: 500
		)
	}
	response {
		status UNPROCESSABLE_ENTITY()
		headers {
			contentType(applicationJson())
		}
		body(
				code: "same_account",
				message: $(anyNonBlankString())
		)
	}
}
