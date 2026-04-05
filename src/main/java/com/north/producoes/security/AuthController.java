package com.north.producoes.security;

import com.north.producoes.controller.api.AuthApi;
import com.north.producoes.controller.dto.request.LoginRequestDTO;
import com.north.producoes.controller.dto.response.LoginResponseDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.security.refreshToken.RefreshRequest;
import com.north.producoes.security.refreshToken.RefreshTokenEntity;
import com.north.producoes.security.refreshToken.RefreshTokenService;
import com.north.producoes.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AuthController implements AuthApi {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public ResponseEntity<LoginResponseDTO> login(LoginRequestDTO loginRequestDTO) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequestDTO.email(), loginRequestDTO.password()));

        UserEntity user = (UserEntity) userService.loadUserByUsername(loginRequestDTO.email());
        String acessToken = jwtService.generateToken(user);
        RefreshTokenEntity refreshToken = refreshTokenService.generate(user);

        return ResponseEntity.ok(new LoginResponseDTO(acessToken, refreshToken.getToken(), user.getRole().name(), user.getId()));
    }

    @Override
    public ResponseEntity<LoginResponseDTO> refresh(RefreshRequest request) {
        RefreshTokenEntity refreshToken = refreshTokenService.validate(request.refreshToken());

        UserEntity user = refreshToken.getUser();
        String newAcessToken = jwtService.generateToken(user);
        return ResponseEntity.ok(new LoginResponseDTO(newAcessToken, request.refreshToken(), user.getRole().name(), user.getId()));
    }
}
