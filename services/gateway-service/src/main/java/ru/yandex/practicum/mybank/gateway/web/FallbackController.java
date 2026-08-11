package ru.yandex.practicum.mybank.gateway.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackController {

	private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

	@RequestMapping("/fallback/{service}")
	public ResponseEntity<ErrorResponse> fallback(@PathVariable String service) {
		log.warn("Route to {} failed, answering with a fallback", service);

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ErrorResponse("service_unavailable", service + " is temporarily unavailable"));
	}
}
