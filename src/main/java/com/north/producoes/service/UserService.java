package com.north.producoes.service;

import com.north.producoes.controller.dto.request.ChangePasswordRequestDTO;
import com.north.producoes.controller.dto.request.RegisterRequestDTO;
import com.north.producoes.controller.dto.request.UserRequestDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.UserRoleEnum;
import com.north.producoes.exception.ResourceAlreadyExistsException;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.FinanceRepository;
import com.north.producoes.repository.PostRepository;
import com.north.producoes.repository.UserRepository;
import com.north.producoes.security.refreshToken.RefreshTokenRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final FinanceRepository financeRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public Page<UserEntity> findAllUser(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
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
            throw new ResourceAlreadyExistsException("Usuario ja cadastrado");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Transactional
    public UserEntity register(RegisterRequestDTO registerRequestDTO) {
        UserEntity user = new UserEntity();
        user.setName(registerRequestDTO.name());
        user.setEmail(registerRequestDTO.email());
        user.setPassword(registerRequestDTO.password());
        user.setRole(UserRoleEnum.USER);
        return saveUser(user);
    }

    @Transactional
    public void deleteUserById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID de Usuario nao encontrado" + id);
        }
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuario nao encontrado com id: " + id);
        }
        refreshTokenRepository.deleteByUserId(id);
        postRepository.nullifyUserByUserId(id);
        financeRepository.nullifyUserByUserId(id);
        userRepository.deleteById(id);
    }

    @Transactional
    public UserEntity updateProfile(Long userId, UserRequestDTO userRequestDTO) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado com id: " + userId));

        if (!user.getEmail().equals(userRequestDTO.email())) {
            if (userRepository.findByEmail(userRequestDTO.email()).isPresent()) {
                throw new IllegalArgumentException("Email ja esta em uso");
            }
            user.setEmail(userRequestDTO.email());
        }

        user.setName(userRequestDTO.name());
        return userRepository.save(user);
    }

    @Transactional
    public UserEntity updateRole(Long userId, UserRoleEnum role) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado com id: " + userId));
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequestDTO dto) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado com id: " + userId));

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Senha atual esta incorreta");
        }

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);
    }
}
