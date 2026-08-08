package contracts.transfers

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "rejects a repeated request while the first one is still being processed"
	priority 1
	request {
		method POST()
		url "/api/transfers"
		headers {
			contentType(applicationJson())
			header("Idempotency-Key", "eeeeeeee-1111-1111-1111-111111111111")
		}
		body(
				toLogin: "user2",
				amount: 500
		)
	}
	response {
		status CONFLICT()
		headers {
			contentType(applicationJson())
		}
		body(
				code: "duplicate_request",
				message: $(anyNonBlankString())
		)
	}
}
