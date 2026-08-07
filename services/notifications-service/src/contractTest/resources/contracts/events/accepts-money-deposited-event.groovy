package contracts.events

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "accepts MONEY_DEPOSITED event: operation carries the credited account and the new balance"
	priority 1
	request {
		method POST()
		url "/api/notifications"
		headers {
			contentType(applicationJson())
		}
		body(
				eventUuid: $(anyUuid()),
				type: "money_deposited",
				recipientUuid: $(anyUuid()),
				payload: [
						transactionUuid: $(anyUuid()),
						operation: [
								toNumber    : $(regex('[0-9]{20}')),
								amount      : 500,
								balanceAfter: 25500
						]
				]
		)
	}
	response {
		status OK()
	}
}
