package contracts.transactions

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "rejects a transaction uuid that was already applied with different details"
	priority 1
	request {
		method POST()
		url "/api/transactions/deposit"
		headers {
			contentType(applicationJson())
		}
		body(
				transactionUuid: "00000000-0000-0000-0000-000000000409",
				login: "user1",
				amount: 500
		)
	}
	response {
		status CONFLICT()
		headers {
			contentType(applicationJson())
		}
		body(
				code: "transaction_conflict",
				message: $(anyNonBlankString())
		)
	}
}
