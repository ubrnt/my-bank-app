package ru.yandex.practicum.mybank.gateway;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.profiles.active=test")
@Import(FakeJwtDecoderConfig.class)
class GatewayRoutingTest {

	private static final String BEARER = "Bearer " + FakeJwtDecoderConfig.USER_TOKEN;

	@RegisterExtension
	static WireMockExtension accounts = WireMockExtension.newInstance()
			.options(options().dynamicPort())
			.build();

	@RegisterExtension
	static WireMockExtension cash = WireMockExtension.newInstance()
			.options(options().dynamicPort())
			.build();

	@RegisterExtension
	static WireMockExtension transfer = WireMockExtension.newInstance()
			.options(options().dynamicPort())
			.build();

	@LocalServerPort
	private int port;

	private WebTestClient client;

	@BeforeEach
	void bindToRunningGateway() {
		client = WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + port)
				.build();
	}

	@DynamicPropertySource
	static void discovery(DynamicPropertyRegistry registry) {
		registry.add("spring.cloud.discovery.client.simple.instances.accounts-service[0].uri", accounts::baseUrl);
		registry.add("spring.cloud.discovery.client.simple.instances.cash-service[0].uri", cash::baseUrl);
		registry.add("spring.cloud.discovery.client.simple.instances.transfer-service[0].uri", transfer::baseUrl);
	}

	@Test
	void routesCustomersToAccountsServiceAndPassesTheTokenOn() {
		accounts.stubFor(get(urlEqualTo("/api/customers/me"))
				.willReturn(jsonResponse("{\"login\":\"user1\"}")));

		client.get().uri("/api/customers/me")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.login").isEqualTo("user1");

		accounts.verify(getRequestedFor(urlEqualTo("/api/customers/me"))
				.withHeader(HttpHeaders.AUTHORIZATION, equalTo(BEARER)));
	}

	@Test
	void routesCashToCashService() {
		cash.stubFor(post(urlEqualTo("/api/cash/deposit"))
				.willReturn(jsonResponse("{\"uuid\":\"11111111-1111-1111-1111-111111111111\"}")));

		client.post().uri("/api/cash/deposit")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"amount\":1500}")
				.exchange()
				.expectStatus().isOk();

		cash.verify(postRequestedFor(urlEqualTo("/api/cash/deposit")));
		accounts.verify(0, anyRequestedFor(anyUrl()));
	}

	@Test
	void routesTransfersToTransferService() {
		transfer.stubFor(post(urlEqualTo("/api/transfers"))
				.willReturn(jsonResponse("{\"uuid\":\"22222222-2222-2222-2222-222222222222\"}")));

		client.post().uri("/api/transfers")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"toLogin\":\"user2\",\"amount\":1500}")
				.exchange()
				.expectStatus().isOk();

		transfer.verify(postRequestedFor(urlEqualTo("/api/transfers")));
	}

	@Test
	void rejectsRequestWithoutToken() {
		client.get().uri("/api/customers/me")
				.exchange()
				.expectStatus().isUnauthorized();

		accounts.verify(0, anyRequestedFor(anyUrl()));
	}

	@Test
	void rejectsTokenItCannotValidate() {
		client.get().uri("/api/customers/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer forged-token")
				.exchange()
				.expectStatus().isUnauthorized();

		accounts.verify(0, anyRequestedFor(anyUrl()));
	}

	@Test
	void rejectsServiceOnlyApi() {
		client.post().uri("/api/transactions/deposit")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.exchange()
				.expectStatus().isForbidden();

		accounts.verify(0, anyRequestedFor(anyUrl()));
	}

	@Test
	void rejectsDirectFallbackCalls() {
		client.get().uri("/fallback/accounts-service")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.exchange()
				.expectStatus().isForbidden();
	}

	@Test
	void answersWithFallbackWhenServiceIsUnreachable() {
		accounts.stubFor(get(urlEqualTo("/api/customers/me"))
				.willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

		client.get().uri("/api/customers/me")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.exchange()
				.expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
				.expectBody()
				.jsonPath("$.code").isEqualTo("service_unavailable")
				.jsonPath("$.message").isEqualTo("accounts-service is temporarily unavailable");
	}

	@Test
	void keepsHealthEndpointOpen() {
		client.get().uri("/actuator/health")
				.exchange()
				.expectStatus().isOk();
	}

	private static ResponseDefinitionBuilder jsonResponse(String body) {
		return aResponse()
				.withStatus(200)
				.withHeader("Content-Type", "application/json")
				.withBody(body);
	}
}
