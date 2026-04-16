package com.north.producoes.controller;

import com.north.producoes.controller.api.UserApi;
import com.north.producoes.controller.dto.request.ChangePasswordRequestDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.controller.dto.request.UserRequestDTO;
import com.north.producoes.controller.dto.response.UserResponseDTO;
import com.north.producoes.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> findAll() {
        List<UserResponseDTO> user = userService.findAllUser()
                .stream()
                .map(UserResponseDTO::from)
                .toList();
        return ResponseEntity.ok(user);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> findUserByEmail(String email) {
        return ResponseEntity.ok(UserResponseDTO.from(userService.findUserByEmail(email).orElseThrow()));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> saveUser(UserRequestDTO request) {
        UserEntity user = new UserEntity();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponseDTO.from(userService.saveUser(user)));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUserById(Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<UserResponseDTO> me(@AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            throw new IllegalStateException("Usuário não autenticado");
        }
        return ResponseEntity.ok(UserResponseDTO.from(user));
    }

    @Override
    public ResponseEntity<UserResponseDTO> updateMe(@Valid UserRequestDTO request, @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            throw new IllegalStateException("Usuário não autenticado");
        }
        return ResponseEntity.ok(UserResponseDTO.from(
                userService.updateProfile(user.getId(), request)));
    }

    @Override
    public ResponseEntity<Void> changePassword(ChangePasswordRequestDTO request, @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            throw new IllegalStateException("Usuário não autenticado");
        }
        userService.changePassword(user.getId(), request);
        return ResponseEntity.noContent().build();
    }
}
