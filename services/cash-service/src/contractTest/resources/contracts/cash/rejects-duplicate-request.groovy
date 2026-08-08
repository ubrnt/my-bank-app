package contracts.cash

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "rejects a repeated request while the first one is still being processed"
	priority 1
	request {
		method POST()
		url "/api/cash/deposit"
		headers {
			contentType(applicationJson())
			header("Idempotency-Key", "dddddddd-1111-1111-1111-111111111111")
		}
		body(
				amount: 1500
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
