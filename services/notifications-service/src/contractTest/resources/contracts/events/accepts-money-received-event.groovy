package contracts.events

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "accepts MONEY_RECEIVED event: operation carries both accounts and the receiver balance"
	request {
		method POST()
		url "/api/notifications"
		headers {
			contentType(applicationJson())
		}
		body(
				eventUuid: $(anyUuid()),
				type: "MONEY_RECEIVED",
				recipientUuid: $(anyUuid()),
				payload: [
						uuid     : $(anyUuid()),
						operation: [
								fromNumber  : $(regex('[0-9]{20}')),
								toNumber    : $(regex('[0-9]{20}')),
								amount      : $(anyPositiveInt()),
								balanceAfter: $(anyPositiveInt())
						]
				]
		)
	}
	response {
		status OK()
	}
}
