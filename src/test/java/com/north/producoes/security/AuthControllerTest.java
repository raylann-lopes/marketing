package com.north.producoes.security;

import com.north.producoes.controller.dto.request.LoginRequestDTO;
import com.north.producoes.controller.dto.request.RegisterRequestDTO;
import com.north.producoes.controller.dto.response.LoginResponseDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.UserRoleEnum;
import com.north.producoes.security.refreshToken.RefreshRequestDTO;
import com.north.producoes.security.refreshToken.RefreshTokenService;
import com.north.producoes.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AuthController")
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("deve autenticar usuário e retornar tokens no login")
    void shouldAuthenticateAndReturnTokensOnLogin() {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO("user@example.com", "password123");
        UserEntity user = user(1L);
        when(userService.loadUserByUsername("user@example.com")).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("access-token");
        when(refreshTokenService.generate(user)).thenReturn("refresh-token");

        // Act
        ResponseEntity<LoginResponseDTO> response = authController.login(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isEqualTo("access-token");
        assertThat(response.getBody().refreshToken()).isEqualTo("refresh-token");
        assertThat(response.getBody().userId()).isEqualTo(1L);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("deve registrar usuário e retornar tokens")
    void shouldRegisterAndReturnTokens() {
        // Arrange
        RegisterRequestDTO request = new RegisterRequestDTO("User", "user@example.com", "password123");
        UserEntity user = user(1L);
        when(userService.register(request)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("access-token");
        when(refreshTokenService.generate(user)).thenReturn("refresh-token");

        // Act
        ResponseEntity<LoginResponseDTO> response = authController.register(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().role()).isEqualTo(UserRoleEnum.USER.name());
    }

    @Test
    @DisplayName("deve rotacionar refresh token e retornar novo access token")
    void shouldRefreshToken() {
        // Arrange
        RefreshRequestDTO request = new RefreshRequestDTO("old-refresh-token");
        UserEntity user = user(1L);
        RefreshTokenService.RotatedRefreshToken rotated = new RefreshTokenService.RotatedRefreshToken(user, "new-refresh-token");
        when(refreshTokenService.rotate("old-refresh-token")).thenReturn(rotated);
        when(jwtService.generateToken(user)).thenReturn("new-access-token");

        // Act
        ResponseEntity<LoginResponseDTO> response = authController.refresh(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isEqualTo("new-access-token");
        assertThat(response.getBody().refreshToken()).isEqualTo("new-refresh-token");
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
