package contracts.validation

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "rejects a money event whose payload lacks the operation details"
	request {
		method POST()
		url "/api/notifications"
		headers {
			contentType(applicationJson())
		}
		body(
				eventUuid: $(anyUuid()),
				type: "money_withdrawn",
				recipientUuid: $(anyUuid()),
				payload: [
						uuid: $(anyUuid())
				]
		)
	}
	response {
		status UNPROCESSABLE_ENTITY()
		headers {
			contentType(applicationJson())
		}
		body(
				code: "invalid_event",
				message: $(anyNonBlankString())
		)
	}
}
