package com.ids.ms_users.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.password")
public record PasswordProperties(int bcryptCost) {
}
