package com.relivio.auth.config;

import com.relivio.auth.entity.User;
import com.relivio.auth.enums.Role;
import com.relivio.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${auth.seed-admin-password:admin123}")
    private String seedAdminPassword;

    @Value("${auth.seed-volunteer-password:vol123}")
    private String seedVolunteerPassword;

    public DataSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public void run(String... args) {
        seedIfMissing("1000000001", "Administrator", "admin@relivio.local", seedAdminPassword, Role.ADMIN);
        seedIfMissing("1000000002", "Relivio Volunteer", "volunteer@relivio.local", seedVolunteerPassword, Role.VOLUNTEER);
    }

    private void seedIfMissing(String phone, String name, String email, String password, Role role) {
        if (userRepository.existsByPhone(phone)) {
            return;
        }
        User user = new User();
        user.setName(name);
        user.setPhone(phone);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        userRepository.save(user);
        log.info("Seeded {} account with phone {}", role, phone);
    }
}
