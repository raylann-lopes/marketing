package com.north.producoes.service;

import com.north.producoes.controller.dto.request.RegisterRequestDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.UserRoleEnum;
import com.north.producoes.exception.ResourceAlreadyExistsException;
import com.north.producoes.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("UserService")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Deve registrar um novo usuário com sucesso")
    void shouldRegisterNewUserSuccessfully() {
        RegisterRequestDTO dto = new RegisterRequestDTO("John Doe", "john@example.com", "password123");
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(dto.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity result = userService.register(dto);

        assertThat(result.getName()).isEqualTo(dto.name());
        assertThat(result.getEmail()).isEqualTo(dto.email());
        assertThat(result.getPassword()).isEqualTo("encodedPassword");
        assertThat(result.getRole()).isEqualTo(UserRoleEnum.USER);
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceAlreadyExistsException quando e-mail já está em uso")
    void shouldThrowWhenEmailAlreadyInUse() {
        RegisterRequestDTO dto = new RegisterRequestDTO("John Doe", "john@example.com", "password123");
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(new UserEntity()));

        assertThatThrownBy(() -> userService.register(dto))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Usuario ja cadastrado");

        verify(userRepository, never()).save(any(UserEntity.class));
    }
}
