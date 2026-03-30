package com.north.producoes.security;

import com.north.producoes.controller.api.AuthApi;
import com.north.producoes.controller.dto.request.LoginRequest;
import com.north.producoes.controller.dto.response.LoginResponse;
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
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(), loginRequest.password()));

        UserEntity user = (UserEntity) userService.loadUserByUsername(loginRequest.email());
        String acessToken = jwtService.generateToken(user);
        RefreshTokenEntity refreshToken = refreshTokenService.generate(user);

        return ResponseEntity.ok(new LoginResponse(acessToken, refreshToken.getToken()));
    }

    @Override
    public ResponseEntity<LoginResponse> refresh(RefreshRequest request) {
        RefreshTokenEntity refreshToken = refreshTokenService.validate(request.refreshToken());

        String newAcessToken = jwtService.generateToken(refreshToken.getUser());
        return ResponseEntity.ok(new LoginResponse(newAcessToken, request.refreshToken()));
    }
}
