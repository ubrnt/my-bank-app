package ru.yandex.practicum.mybank.chassis.web;

import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtLogin {

	private static final String LOGIN_CLAIM = "preferred_username";

	private JwtLogin() {
	}

	public static String of(Jwt jwt) {
		String login = jwt.getClaimAsString(LOGIN_CLAIM);

		if (login == null || login.isBlank()) {
			throw new MissingUsernameClaimException();
		}

		return login;
	}
}
