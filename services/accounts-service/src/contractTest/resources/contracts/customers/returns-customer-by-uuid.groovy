package contracts.customers

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "returns customer login and name by uuid"
	priority 2
	request {
		method GET()
		url $(consumer(regex('/api/customers/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}')),
				producer('/api/customers/3fa85f64-5717-4562-b3fc-2c963f66afa6'))
	}
	response {
		status OK()
		headers {
			contentType(applicationJson())
		}
		body(
				login: "user1",
				name: "user1_first_name user1_last_name"
		)
	}
}
