package com.ids.ms_users.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    @Bean
    PasswordEncoder passwordEncoder(@Value("${app.security.password.bcrypt-cost:10}") int cost) {
        return new BCryptPasswordEncoder(cost);
    }
}
