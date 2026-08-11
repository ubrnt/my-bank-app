package contracts.customers

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "responds with not found for an unknown customer uuid"
	priority 1
	request {
		method GET()
		url "/api/customers/00000000-0000-0000-0000-000000000404"
	}
	response {
		status NOT_FOUND()
		headers {
			contentType(applicationJson())
		}
		body(
				code: "customer_account_not_found",
				message: $(anyNonBlankString())
		)
	}
}
