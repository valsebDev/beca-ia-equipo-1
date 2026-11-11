package com.ids.ms_users.bootstrap;

import com.ids.ms_users.domain.Role;
import com.ids.ms_users.repository.UserRepository;
import com.ids.ms_users.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrap(UserRepository userRepository,
                          UserService userService,
                          @Value("${app.bootstrap.admin.email:}") String adminEmail,
                          @Value("${app.bootstrap.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            LOG.warn("No existe ADMIN y faltan ADMIN_EMAIL/ADMIN_PASSWORD; no se creó el administrador inicial");
            return;
        }
        userService.createAdminIfAbsent(adminEmail, adminPassword);
        LOG.info("Administrador inicial creado");
    }
}
