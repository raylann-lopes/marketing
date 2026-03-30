package com.north.producoes.service;

import com.north.producoes.entity.UserEntity;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public List<UserEntity> findAllUser() {
        return userRepository.findAll();
    }

    @Override
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado: " + email));
    }

    public Optional<UserEntity> findUserByEmail(String email) {
        if (email == null || email.isBlank()){
            throw new IllegalArgumentException("Email nao pode ser nulo ou vazio");
        }
        return userRepository.findByEmail(email);
    }

    @Transactional
    public UserEntity saveUser(UserEntity user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new ResourceNotFoundException("Usuario ja cadastrado");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUserById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID de Usuario nao encontrado" + id);
        }
        userRepository.deleteById(id);
    }
}
