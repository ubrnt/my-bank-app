package contracts.cash

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "takes money from the account of the logged in customer"
	priority 2
	request {
		method POST()
		url "/api/cash/withdraw"
		headers {
			contentType(applicationJson())
		}
		body(
				amount: 500
		)
	}
	response {
		status OK()
		headers {
			contentType(applicationJson())
		}
		body(
				uuid: $(anyUuid()),
				type: "withdraw",
				amount: 500,
				status: "completed"
		)
	}
}
