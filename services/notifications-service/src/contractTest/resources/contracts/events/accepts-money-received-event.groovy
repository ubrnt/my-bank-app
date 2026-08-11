package contracts.events

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "accepts MONEY_RECEIVED event: operation carries both accounts and the receiver balance"
	priority 1
	request {
		method POST()
		url "/api/notifications"
		headers {
			contentType(applicationJson())
		}
		body(
				eventUuid: $(anyUuid()),
				type: "money_received",
				recipientUuid: $(anyUuid()),
				payload: [
						transactionUuid: $(anyUuid()),
						operation: [
								fromNumber  : $(regex('[0-9]{20}')),
								toNumber    : $(regex('[0-9]{20}')),
								amount      : 500,
								balanceAfter: 5500
						]
				]
		)
	}
	response {
		status OK()
	}
}
