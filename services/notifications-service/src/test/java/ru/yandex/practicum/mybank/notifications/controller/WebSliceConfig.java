package ru.yandex.practicum.mybank.notifications.controller;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.mybank.notifications.config.SecurityConfig;

@TestConfiguration(proxyBeanMethods = false)
@EnableConfigurationProperties(WebEndpointProperties.class)
@Import(SecurityConfig.class)
class WebSliceConfig {
}
