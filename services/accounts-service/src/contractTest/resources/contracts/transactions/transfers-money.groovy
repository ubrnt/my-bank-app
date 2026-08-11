package contracts.transactions

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "applies a transfer and returns both sides of the transaction"
	priority 2
	request {
		method POST()
		url "/api/transactions/transfer"
		headers {
			contentType(applicationJson())
		}
		body(
				transactionUuid: $(anyUuid()),
				fromLogin: "user1",
				toLogin: "user2",
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
				type: "transfer",
				operations: [
						[
								direction: "withdraw",
								fromNumber: "40817810000000000001",
								fromAccountUuid: "11111111-1111-1111-1111-111111111111",
								fromCustomerUuid: "aaaaaaaa-1111-1111-1111-111111111111",
								toNumber: "40817810000000000002",
								toAccountUuid: "22222222-2222-2222-2222-222222222222",
								toCustomerUuid: "aaaaaaaa-2222-2222-2222-222222222222",
								amount: 500,
								balanceAfter: 24500
						],
						[
								direction: "deposit",
								fromNumber: "40817810000000000001",
								fromAccountUuid: "11111111-1111-1111-1111-111111111111",
								fromCustomerUuid: "aaaaaaaa-1111-1111-1111-111111111111",
								toNumber: "40817810000000000002",
								toAccountUuid: "22222222-2222-2222-2222-222222222222",
								toCustomerUuid: "aaaaaaaa-2222-2222-2222-222222222222",
								amount: 500,
								balanceAfter: 5500
						]
				]
		)
	}
}
