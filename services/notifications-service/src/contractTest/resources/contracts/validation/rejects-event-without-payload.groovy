package contracts.validation

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "rejects an event without payload listing the missing field"
	request {
		method POST()
		url "/api/notifications"
		headers {
			contentType(applicationJson())
		}
		body(
				eventUuid: $(anyUuid()),
				type: "money_deposited",
				recipientUuid: $(anyUuid())
		)
	}
	response {
		status BAD_REQUEST()
		headers {
			contentType(applicationJson())
		}
		body(
				code: "validation_error",
				message: "Request validation failed",
				validationErrors: [
						fields: [[field: "payload", message: "must not be null"]]
				]
		)
	}
}
