package contracts.events

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "accepts PROFILE_UPDATED event: payload carries the updated customer"
	request {
		method POST()
		url "/api/notifications"
		headers {
			contentType(applicationJson())
		}
		body(
				eventUuid: $(anyUuid()),
				type: "profile_updated",
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
