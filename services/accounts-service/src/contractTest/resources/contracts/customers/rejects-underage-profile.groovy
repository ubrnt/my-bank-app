package contracts.customers

import org.springframework.cloud.contract.spec.Contract

Contract.make {
	description "rejects a profile of an underage customer and names the offending field"
	request {
		method PUT()
		url "/api/customers/me"
		headers {
			contentType(applicationJson())
		}
		body(
				name: "Иванов Иван",
				birthdate: "2020-01-15"
		)
	}
	response {
		status BAD_REQUEST()
		headers {
			contentType(applicationJson())
		}
		body(
				code: "validation_error",
				message: "Request validation failed",
				validationErrors: [
						fields: [
								[
										field  : "birthdate",
										message: "must be at least 18 years old"
								]
						]
				]
		)
	}
}
