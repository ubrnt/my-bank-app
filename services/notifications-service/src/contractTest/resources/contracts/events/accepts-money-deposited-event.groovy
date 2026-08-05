package contracts.events

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "accepts MONEY_DEPOSITED event: operation carries the credited account and the new balance"
	request {
		method POST()
		url "/api/notifications"
		headers {
			contentType(applicationJson())
		}
		body(
				eventUuid: $(anyUuid()),
				type: "MONEY_DEPOSITED",
				recipientUuid: $(anyUuid()),
				payload: [
						uuid     : $(anyUuid()),
						operation: [
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
