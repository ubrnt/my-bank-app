package contracts.events

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "accepts MONEY_SENT event: operation carries both accounts and the sender balance"
	priority 1
	request {
		method POST()
		url "/api/notifications"
		headers {
			contentType(applicationJson())
		}
		body(
				eventUuid: $(anyUuid()),
				type: "money_sent",
				recipientUuid: $(anyUuid()),
				payload: [
						transactionUuid: $(anyUuid()),
						operation: [
								fromNumber  : $(regex('[0-9]{20}')),
								toNumber    : $(regex('[0-9]{20}')),
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
