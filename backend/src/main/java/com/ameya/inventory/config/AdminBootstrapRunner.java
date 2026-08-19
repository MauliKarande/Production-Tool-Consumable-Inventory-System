package com.ameya.inventory.config;

import com.ameya.inventory.entity.Role;
import com.ameya.inventory.entity.RoleName;
import com.ameya.inventory.entity.User;
import com.ameya.inventory.repository.RoleRepository;
import com.ameya.inventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first Admin login on an empty database so the system is
 * usable immediately after migration, without ever committing a
 * password hash to version control. Runs once - if any ADMIN user
 * already exists, this is a no-op.
 */
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.username:admin}")
    private String defaultUsername;

    @Value("${app.bootstrap-admin.password:#{null}}")
    private String defaultPassword;

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole_Name(RoleName.ADMIN)) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role missing - check V2 seed migration"));

        String password = (defaultPassword != null && !defaultPassword.isBlank())
                ? defaultPassword
                : "Ameya@" + (int) (Math.random() * 900000 + 100000);

        User admin = new User();
        admin.setUsername(defaultUsername);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(adminRole);
        admin.setActive(true);
        userRepository.save(admin);

        log.warn("=================================================================");
        log.warn(" No ADMIN user existed - created one automatically:");
        log.warn("   username: {}", defaultUsername);
        if (defaultPassword != null && !defaultPassword.isBlank()) {
            log.warn("   password: (from APP_BOOTSTRAP_ADMIN_PASSWORD env var)");
        } else {
            log.warn("   password: {}", password);
            log.warn("   This random password is shown ONLY here, ONCE. Log in and");
            log.warn("   change it immediately, or set APP_BOOTSTRAP_ADMIN_PASSWORD");
            log.warn("   before startup next time.");
        }
        log.warn("=================================================================");
    }
}
