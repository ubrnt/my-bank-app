package contracts.transactions

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "applies a deposit and returns the resulting transaction"
	priority 2
	request {
		method POST()
		url "/api/transactions/deposit"
		headers {
			contentType(applicationJson())
		}
		body(
				transactionUuid: $(anyUuid()),
				login: "user1",
				amount: 500
		)
	}
	response {
		status OK()
		headers {
			contentType(applicationJson())
		}
		body(
				uuid: fromRequest().body('$.transactionUuid'),
				type: "deposit",
				operations: [
						[
								direction: "deposit",
								toNumber: "40817810000000000001",
								toAccountUuid: "11111111-1111-1111-1111-111111111111",
								toCustomerUuid: "aaaaaaaa-1111-1111-1111-111111111111",
								amount: 500,
								balanceAfter: 25500
						]
				]
		)
	}
}
