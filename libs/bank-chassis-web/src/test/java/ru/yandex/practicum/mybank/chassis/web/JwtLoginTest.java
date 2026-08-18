package ru.yandex.practicum.mybank.chassis.web;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtLoginTest {

	@Test
	void readsTheLoginClaim() {
		assertThat(JwtLogin.of(tokenWith(Map.of("preferred_username", "user1")))).isEqualTo("user1");
	}

	@Test
	void rejectsATokenWithoutTheLoginClaim() {
		assertThatThrownBy(() -> JwtLogin.of(tokenWith(Map.of("sub", "user1"))))
				.isInstanceOf(MissingUsernameClaimException.class);
	}

	@Test
	void rejectsABlankLoginClaim() {
		assertThatThrownBy(() -> JwtLogin.of(tokenWith(Map.of("preferred_username", " "))))
				.isInstanceOf(MissingUsernameClaimException.class);
	}

	private Jwt tokenWith(Map<String, Object> claims) {
		return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
				Map.of("alg", "none"), claims);
	}
}
