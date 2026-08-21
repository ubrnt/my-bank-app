package ru.yandex.practicum.mybank.front.config;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, WebEndpointProperties endpoints,
			ClientRegistrationRepository clientRegistrations) throws Exception {
		String healthPath = endpoints.getBasePath() + "/health/**";
		String prometheusPath = endpoints.getBasePath() + "/prometheus";
		return http
				.authorizeHttpRequests(requests -> requests
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers(HttpMethod.GET, healthPath).permitAll()
						.requestMatchers(HttpMethod.GET, prometheusPath).permitAll()
						.anyRequest().authenticated())
				.oauth2Login(Customizer.withDefaults())
				.logout(logout -> logout.logoutSuccessHandler(logoutSuccessHandler(clientRegistrations)))
				.build();
	}

	private LogoutSuccessHandler logoutSuccessHandler(ClientRegistrationRepository clientRegistrations) {
		OidcClientInitiatedLogoutSuccessHandler handler =
				new OidcClientInitiatedLogoutSuccessHandler(clientRegistrations);
		handler.setPostLogoutRedirectUri("{baseUrl}");

		return handler;
	}
}
