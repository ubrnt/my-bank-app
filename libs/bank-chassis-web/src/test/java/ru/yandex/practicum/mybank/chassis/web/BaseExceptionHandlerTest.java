package ru.yandex.practicum.mybank.chassis.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BaseExceptionHandlerTest {

	private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
			.setControllerAdvice(new TestExceptionHandler())
			.build();

	@Test
	void keepsAcceptHeaderWhenTheContentTypeIsNotSupported() throws Exception {
		mockMvc.perform(post("/test").contentType(MediaType.TEXT_PLAIN).content("boom"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(header().string(HttpHeaders.ACCEPT, containsString(MediaType.APPLICATION_JSON_VALUE)))
				.andExpect(jsonPath("$.code").value("unsupported_media_type"));
	}

	@Test
	void reportsUnexpectedFailureInTheCommonFormat() throws Exception {
		mockMvc.perform(post("/test").contentType(MediaType.APPLICATION_JSON).content("""
						{"fail": true}
						"""))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("internal_error"))
				.andExpect(jsonPath("$.message").value("Request processing failed"));
	}

	@RestController
	static class TestController {

		@PostMapping(path = "/test", consumes = MediaType.APPLICATION_JSON_VALUE)
		public String handle(@RequestBody Map<String, Object> body) {
			throw new IllegalStateException("boom");
		}
	}

	@RestControllerAdvice
	static class TestExceptionHandler extends BaseExceptionHandler {
	}
}
