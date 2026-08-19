package ru.yandex.practicum.mybank.accounts;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ru.yandex.practicum.mybank.notifications.outbox.NotificationsEventPublisher;

import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresContainerConfig.class)
public abstract class AbstractIntegrationTest {

	protected static final long INITIAL_BALANCE = 100_000;

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@MockitoBean
	protected NotificationsEventPublisher notificationsEventPublisher;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	private List<Map<String, Object>> profiles;

	@BeforeEach
	void rememberProfiles() {
		profiles = jdbcTemplate.queryForList("select login, name, birthdate from customers");
	}

	@AfterEach
	void resetData() {
		jdbcTemplate.execute("truncate table balance_operations, transactions, notifications_outbox restart identity");
		jdbcTemplate.update("update accounts set balance = ?", INITIAL_BALANCE);

		profiles.forEach(profile -> jdbcTemplate.update(
				"update customers set name = ?, birthdate = ? where login = ?",
				profile.get("name"), profile.get("birthdate"), profile.get("login")));
	}

	protected static RequestPostProcessor serviceToken() {
		return jwt().jwt(jwt -> jwt.claim("scope", "transactions:write"));
	}

	protected long balanceOf(String login) {
		return jdbcTemplate.queryForObject("""
				select a.balance from accounts a join customers c on c.id = a.customer_id where c.login = ?
				""", Long.class, login);
	}

	protected long countOf(String table) {
		return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
	}
}
