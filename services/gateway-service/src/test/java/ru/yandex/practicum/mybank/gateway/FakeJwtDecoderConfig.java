package ru.yandex.practicum.mybank.gateway;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

import java.time.Instant;

@TestConfiguration(proxyBeanMethods = false)
class FakeJwtDecoderConfig {

	static final String USER_TOKEN = "user-token";

	@Bean
	public ReactiveJwtDecoder reactiveJwtDecoder() {
		return token -> {
			if (!USER_TOKEN.equals(token)) {
				return Mono.error(new BadJwtException("Unexpected token " + token));
			}

			Instant issuedAt = Instant.now();

			return Mono.just(Jwt.withTokenValue(token)
					.header("alg", "none")
					.subject("user1")
					.claim("preferred_username", "user1")
					.issuedAt(issuedAt)
					.expiresAt(issuedAt.plusSeconds(3600))
					.build());
		};
	}
}
