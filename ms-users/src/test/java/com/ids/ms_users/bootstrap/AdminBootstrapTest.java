package com.ids.ms_users.bootstrap;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ids.ms_users.config.properties.BootstrapAdminProperties;
import com.ids.ms_users.domain.Role;
import com.ids.ms_users.repository.UserRepository;
import com.ids.ms_users.service.UserService;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

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

    private AdminBootstrap bootstrap(String email, String password) {
        return new AdminBootstrap(userRepository, userService,
                new BootstrapAdminProperties(email, password));
    }
}
