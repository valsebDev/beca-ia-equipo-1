package com.ids.ms_users.bootstrap;

import com.ids.ms_users.domain.Role;
import com.ids.ms_users.repository.UserRepository;
import com.ids.ms_users.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.ids.ms_users.config.properties.BootstrapAdminProperties;

@Component
@EnableConfigurationProperties(BootstrapAdminProperties.class)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final String PLACEHOLDER_PREFIX = "${";

    private final UserRepository userRepository;
    private final UserService userService;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrap(UserRepository userRepository,
                          UserService userService,
                          BootstrapAdminProperties properties) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.adminEmail = properties.email();
        this.adminPassword = properties.password();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        if (!isUsable(adminEmail) || !isUsable(adminPassword)) {
            LOG.warn("No existe ADMIN y faltan ADMIN_EMAIL/ADMIN_PASSWORD; no se creó el administrador inicial");
            return;
        }
        userService.createAdminIfAbsent(adminEmail, adminPassword);
        LOG.info("Administrador inicial creado");
    }

    // Si la variable de entorno no existe, Spring deja el placeholder "${...}" como texto literal.
    private static boolean isUsable(String value) {
        return value != null && !value.isBlank() && !value.startsWith(PLACEHOLDER_PREFIX);
    }
}
