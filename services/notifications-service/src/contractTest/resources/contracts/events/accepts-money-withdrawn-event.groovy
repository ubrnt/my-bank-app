package contracts.events

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "accepts MONEY_WITHDRAWN event: operation carries the debited account and the new balance"
	priority 1
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
						transactionUuid: $(anyUuid()),
						operation: [
								fromNumber  : $(regex('[0-9]{20}')),
								amount      : 500,
								balanceAfter: 24500
						]
				]
		)
	}
	response {
		status OK()
	}
}
