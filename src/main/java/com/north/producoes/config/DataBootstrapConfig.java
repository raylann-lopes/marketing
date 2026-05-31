package com.north.producoes.config;

import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.UserRoleEnum;
import com.north.producoes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataBootstrapConfig implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${bootstrap.admin.name}")
    private String adminName;

    @Value("${bootstrap.admin.email}")
    private String adminEmail;

    @Value("${bootstrap.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail == null || adminEmail.isBlank()
                || adminName == null || adminName.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            log.info("Bootstrap admin credentials incomplete, skipping admin bootstrap.");
            return;
        }

        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin user already exists, skipping bootstrap.");
            return;
        }

        UserEntity admin = new UserEntity();
        admin.setName(adminName);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(UserRoleEnum.ADMIN);

        userRepository.save(admin);
        log.info("Admin user '{}' created via bootstrap.", adminEmail);
    }
}
