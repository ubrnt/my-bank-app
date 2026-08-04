package ru.yandex.practicum.mybank.accounts.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "mybank.outbox.enabled", matchIfMissing = true)
public class SchedulingConfig {
}
