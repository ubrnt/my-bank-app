package contracts.notifications

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "publishes an event when money is put on an account"
	label "money_deposited_event"
	input {
		triggeredBy("publishMoneyDeposited()")
	}
	outputMessage {
		sentTo "notifications"
		body(
				eventUuid: "1b7f4a90-0d51-4c2e-9f77-0a1e5c3b0002",
				type: "money_deposited",
				recipientUuid: "aaaaaaaa-1111-1111-1111-111111111111",
				payload: [
						transactionUuid: "bbbbbbbb-1111-1111-1111-111111111111",
						type: "deposit",
						operation: [
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
