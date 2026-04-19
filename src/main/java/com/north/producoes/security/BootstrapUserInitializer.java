package com.north.producoes.security;

import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.UserRoleEnum;
import com.north.producoes.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "app.bootstrap.user.enabled", havingValue = "true")
public class BootstrapUserInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.user.name:}")
    private String name;

    @Value("${app.bootstrap.user.email:}")
    private String email;

    @Value("${app.bootstrap.user.password:}")
    private String password;

    @Value("${app.bootstrap.user.role:ADMIN}")
    private UserRoleEnum role;

    public BootstrapUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validateConfiguration();

        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Bootstrap user already exists: {}", email);
            return;
        }

        UserEntity user = new UserEntity();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);

        userRepository.save(user);
        log.info("Bootstrap user created successfully: {} ({})", email, role);
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(name)) {
            throw new IllegalStateException("app.bootstrap.user.name is required when bootstrap user is enabled");
        }
        if (!StringUtils.hasText(email)) {
            throw new IllegalStateException("app.bootstrap.user.email is required when bootstrap user is enabled");
        }
        if (!StringUtils.hasText(password)) {
            throw new IllegalStateException("app.bootstrap.user.password is required when bootstrap user is enabled");
        }
    }
}
