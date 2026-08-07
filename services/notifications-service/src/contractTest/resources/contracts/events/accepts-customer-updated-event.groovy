package contracts.events

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "accepts CUSTOMER_UPDATED event: payload carries the updated customer"
	priority 1
	request {
		method POST()
		url "/api/notifications"
		headers {
			contentType(applicationJson())
		}
		body(
				eventUuid: $(anyUuid()),
				type: "customer_updated",
				recipientUuid: $(anyUuid()),
				payload: [
						uuid: $(anyUuid())
				]
		)
	}
	response {
		status OK()
	}
}
