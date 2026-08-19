package contracts.notifications

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "publishes an event when the profile of a customer is updated"
	label "customer_updated_event"
	input {
		triggeredBy("publishCustomerUpdated()")
	}
	outputMessage {
		sentTo "notifications"
		body(
				eventUuid: "1b7f4a90-0d51-4c2e-9f77-0a1e5c3b0001",
				type: "customer_updated",
				recipientUuid: "aaaaaaaa-1111-1111-1111-111111111111",
				payload: [
						customerUuid: "aaaaaaaa-1111-1111-1111-111111111111"
				]
		)
	}
}
