package ru.yandex.practicum.mybank.accounts.config;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, WebEndpointProperties endpoints) throws Exception {
		String healthPath = endpoints.getBasePath() + "/health/**";
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(requests -> requests
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers(HttpMethod.GET, healthPath).permitAll()
						.requestMatchers(HttpMethod.GET, "/api/customers/others").hasAuthority("SCOPE_customer:others:read")
						.requestMatchers(HttpMethod.GET, "/api/customers/me").hasAuthority("SCOPE_customer:read")
						.requestMatchers(HttpMethod.PUT, "/api/customers/me").hasAuthority("SCOPE_customer:write")
						.requestMatchers(HttpMethod.GET, "/api/customers/*").hasAuthority("SCOPE_customer:any:read")
						.requestMatchers("/api/transactions/**").hasAuthority("SCOPE_transactions:write")
						.anyRequest().denyAll())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
				.build();
	}
}
