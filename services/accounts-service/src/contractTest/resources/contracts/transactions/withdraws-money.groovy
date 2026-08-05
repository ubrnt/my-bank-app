package contracts.transactions

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "applies a withdrawal and returns the resulting transaction"
	priority 2
	request {
		method POST()
		url "/api/transactions/withdraw"
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
				type: "withdraw",
				operation: [
						direction: "withdraw",
						fromNumber: "40817810000000000001",
						fromAccountUuid: "11111111-1111-1111-1111-111111111111",
						fromCustomerUuid: "aaaaaaaa-1111-1111-1111-111111111111",
						amount: 500,
						balanceAfter: 24500
				]
		)
	}
}
