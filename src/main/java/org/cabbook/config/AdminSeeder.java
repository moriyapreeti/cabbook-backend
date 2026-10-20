package org.cabbook.config;

import org.cabbook.user.Role;
import org.cabbook.user.User;
import org.cabbook.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first admin from ADMIN_EMAIL / ADMIN_PASSWORD env vars (admins cannot self-register). */
@Component
public class AdminSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email, password;

    public AdminSeeder(UserRepository users, PasswordEncoder encoder,
                       @Value("${cabbook.admin.email:}") String email, @Value("${cabbook.admin.password:}") String password) {
        this.users = users; this.encoder = encoder; this.email = email; this.password = password;
    }

    @Override
    public void run(String... args) {
        if (email.isBlank() || password.length() < 8 || users.existsByEmailIgnoreCase(email)) return;
        User u = new User();
        u.setName("Admin");
        u.setEmail(email.toLowerCase());
        u.setPasswordHash(encoder.encode(password));
        u.setRole(Role.ADMIN);
        users.save(u);
        log.info("Seeded admin user {}", email);
    }
}
