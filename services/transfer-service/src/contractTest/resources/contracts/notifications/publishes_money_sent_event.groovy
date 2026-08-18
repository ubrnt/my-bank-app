package contracts.notifications

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "publishes an event for the sender when money is transferred"
	label "money_sent_event"
	input {
		triggeredBy("publishMoneySent()")
	}
	outputMessage {
		sentTo "notifications"
		body(
				eventUuid: "1b7f4a90-0d51-4c2e-9f77-0a1e5c3b0003",
				type: "money_sent",
				recipientUuid: "aaaaaaaa-1111-1111-1111-111111111111",
				payload: [
						transactionUuid: "bbbbbbbb-1111-1111-1111-111111111111",
						type: "transfer",
						operation: [
								direction: "withdraw",
								fromNumber: "40817810000000000001",
								fromAccountUuid: "11111111-1111-1111-1111-111111111111",
								fromCustomerUuid: "aaaaaaaa-1111-1111-1111-111111111111",
								toNumber: "40817810000000000002",
								toAccountUuid: "22222222-2222-2222-2222-222222222222",
								toCustomerUuid: "aaaaaaaa-2222-2222-2222-222222222222",
								amount: 500,
								balanceAfter: 24500
						]
				]
		)
	}
}
