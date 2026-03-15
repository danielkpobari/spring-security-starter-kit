package com.conclase.app.config;

import com.conclase.app.entity.User;
import com.conclase.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(new User(null, "john", passwordEncoder.encode("password123"), "ROLE_USER"));
            userRepository.save(new User(null, "admin", passwordEncoder.encode("password123"), "ROLE_ADMIN,ROLE_USER"));
            userRepository.save(new User(null, "jane", passwordEncoder.encode("password123"), "ROLE_USER"));
        }
    }
}
