package com.ids.ms_users.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ids.ms_users.config.properties.PasswordProperties;

@Configuration
@EnableConfigurationProperties(PasswordProperties.class)
public class PasswordEncoderConfig {

    @Bean
    PasswordEncoder passwordEncoder(PasswordProperties properties) {
        return new BCryptPasswordEncoder(properties.bcryptCost());
    }
}
