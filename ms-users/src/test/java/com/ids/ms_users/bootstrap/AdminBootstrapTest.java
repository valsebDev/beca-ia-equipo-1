package com.ids.ms_users.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.ids.ms_users.config.properties.BootstrapAdminProperties;
import com.ids.ms_users.domain.Role;
import com.ids.ms_users.repository.UserRepository;
import com.ids.ms_users.service.UserService;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "Admin-Secreta-123";
    private static final String CREATED_MESSAGE = "Administrador inicial creado";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

    @Test
    void doesNothingWhenAnAdminAlreadyExists() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

        bootstrap("admin@example.com", "secret").run(null);

        verify(userService, never()).createAdminIfAbsent(anyString(), anyString());
    }

    @Test
    void skipsCreationWhenCredentialsAreMissing() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        bootstrap("", "").run(null);

        verify(userService, never()).createAdminIfAbsent(anyString(), anyString());
    }

    @Test
    void createsAdminWhenNoneExistsAndCredentialsAreProvided() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        bootstrap("admin@example.com", "secret").run(null);

        verify(userService).createAdminIfAbsent("admin@example.com", "secret");
    }

    @Test
    void skipsCreationWhenPasswordPlaceholderIsUnresolved() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        bootstrap(ADMIN_EMAIL, "${ADMIN_PASSWORD}").run(null);

        verify(userService, never()).createAdminIfAbsent(anyString(), anyString());
    }

    @Test
    void neverLogsAdminPassword() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        Logger logger = (Logger) LoggerFactory.getLogger(AdminBootstrap.class);
        logAppender.start();
        logger.addAppender(logAppender);

        bootstrap(ADMIN_EMAIL, ADMIN_PASSWORD).run(null);

        List<String> messages = logAppender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).contains(CREATED_MESSAGE);
        assertThat(messages).noneMatch(message -> message.contains(ADMIN_PASSWORD));
    }

    @AfterEach
    void detachLogAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(AdminBootstrap.class);
        logger.detachAppender(logAppender);
        logAppender.stop();
    }

    private AdminBootstrap bootstrap(String email, String password) {
        return new AdminBootstrap(userRepository, userService,
                new BootstrapAdminProperties(email, password));
    }
}
