package com.north.producoes.security;

import com.north.producoes.controller.dto.request.LoginRequestDTO;
import com.north.producoes.controller.dto.request.RegisterRequestDTO;
import com.north.producoes.controller.dto.response.LoginResponseDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.security.refreshToken.RefreshRequestDTO;
import com.north.producoes.security.refreshToken.RefreshTokenService;
import com.north.producoes.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO loginRequestDTO) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequestDTO.email(), loginRequestDTO.password()));

        UserEntity user = userService.findUserByEmail(loginRequestDTO.email())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String acessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.generate(user);

        return ResponseEntity.ok(new LoginResponseDTO(acessToken, refreshToken, user.getRole().name(), user.getId()));
    }

    @PostMapping("/register")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<LoginResponseDTO> register(@RequestBody @Valid RegisterRequestDTO registerRequestDTO) {
        UserEntity user = userService.register(registerRequestDTO);
        String acessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.generate(user);

        return ResponseEntity.ok(new LoginResponseDTO(acessToken, refreshToken, user.getRole().name(), user.getId()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refresh(@RequestBody @Valid RefreshRequestDTO request) {
        RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(request.refreshToken());

        UserEntity user = rotated.user();
        String accessToken = jwtService.generateToken(user);

        return ResponseEntity.ok(new LoginResponseDTO(accessToken, rotated.refreshToken(), user.getRole().name(), user.getId()));
    }
}
