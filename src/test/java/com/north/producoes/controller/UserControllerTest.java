package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.ChangePasswordRequestDTO;
import com.north.producoes.controller.dto.request.UserRequestDTO;
import com.north.producoes.controller.dto.response.UserResponseDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.UserRoleEnum;
import com.north.producoes.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("UserController")
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    @DisplayName("deve retornar todos os usuários")
    void shouldReturnAllUsers() {
        // Arrange
        when(userService.findAllUser(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user(1L))));

        // Act
        ResponseEntity<List<UserResponseDTO>> response = userController.findAll(0, 50);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .hasSize(1)
                .extracting(UserResponseDTO::id)
                .containsExactly(1L);
    }

    @Test
    @DisplayName("deve retornar usuário por email")
    void shouldReturnUserByEmail() {
        // Arrange
        when(userService.findUserByEmail("user@example.com")).thenReturn(Optional.of(user(1L)));

        // Act
        ResponseEntity<UserResponseDTO> response = userController.findUserByEmail("user@example.com");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("deve criar usuário com status 201")
    void shouldCreateUserWithCreatedStatus() {
        // Arrange
        UserRequestDTO request = new UserRequestDTO("User", "user@example.com", "password123");
        when(userService.saveUser(any(UserEntity.class))).thenReturn(user(1L));

        // Act
        ResponseEntity<UserResponseDTO> response = userController.saveUser(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("deve retornar usuário autenticado")
    void shouldReturnAuthenticatedUser() {
        // Act
        ResponseEntity<UserResponseDTO> response = userController.findMe(user(1L));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("deve lançar IllegalStateException quando usuário autenticado for nulo")
    void shouldThrowWhenAuthenticatedUserIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> userController.findMe(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não autenticado");
    }

    @Test
    @DisplayName("deve alterar senha do usuário autenticado com status 204")
    void shouldChangeAuthenticatedUserPassword() {
        // Arrange
        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO("old", "newPassword123");
        UserEntity user = user(1L);

        // Act
        ResponseEntity<Void> response = userController.changePassword(request, user);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).changePassword(1L, request);
    }

    private static UserEntity user(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setName("User");
        user.setEmail("user@example.com");
        user.setRole(UserRoleEnum.USER);
        return user;
    }
}
