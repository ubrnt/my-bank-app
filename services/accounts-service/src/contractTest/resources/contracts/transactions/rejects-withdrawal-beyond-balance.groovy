package contracts.transactions

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "rejects a withdrawal that exceeds the available balance"
	priority 1
	request {
		method POST()
		url "/api/transactions/withdraw"
		headers {
			contentType(applicationJson())
		}
		body(
				transactionUuid: $(anyUuid()),
				login: "user1",
				amount: 1000000000000
		)
	}
	response {
		status UNPROCESSABLE_ENTITY()
		headers {
			contentType(applicationJson())
		}
		body(
				code: "insufficient_funds",
				message: $(anyNonBlankString())
		)
	}
}
