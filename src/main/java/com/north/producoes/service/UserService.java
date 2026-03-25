package com.north.producoes.service;

import com.north.producoes.entity.UserEntity;
import com.north.producoes.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@AllArgsConstructor
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private UserRepository userRepository;

    public List<UserEntity> findAllUser() {
        return userRepository.findAll();
    }

    public UserEntity findById(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));
    }

    public UserEntity findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));
    }

    @Transactional
    public UserEntity saveUser(UserEntity user) {
        if (userRepository.findByEmail(user.getEmail()).isEmpty()) {
            throw new RuntimeException("Usuario ja cadastrado");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUserById(Long id) {
        if (userRepository.findById(id).isEmpty()) {
            throw new RuntimeException("Usuario nao encontrado");
        }
        userRepository.deleteById(id);
    }
}
