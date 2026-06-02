package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.ChangePasswordRequestDTO;
import com.north.producoes.controller.dto.request.UpdateRoleRequestDTO;
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
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 200));
        List<UserResponseDTO> users = userService.findAllUser(pageable)
                .map(UserResponseDTO::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<UserResponseDTO> findUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(UserResponseDTO.from(userService.findUserByEmail(email).orElseThrow()));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<UserResponseDTO> saveUser(@Valid @RequestBody UserRequestDTO request) {
        UserEntity user = new UserEntity();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponseDTO.from(userService.saveUser(user)));
    }

    @PatchMapping("/id/{id}/role")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<UserResponseDTO> updateRole(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateRoleRequestDTO request,
                                                      @AuthenticationPrincipal UserEntity currentUser) {
        if (currentUser.getId().equals(id)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(UserResponseDTO.from(userService.updateRole(id, request.role())));
    }

    @DeleteMapping("/id/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<Void> deleteUserById(@PathVariable Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> findMe(@AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            throw new IllegalStateException("Usuário não autenticado");
        }
        return ResponseEntity.ok(UserResponseDTO.from(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> updateMe(@Valid @RequestBody UserRequestDTO request, @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            throw new IllegalStateException("Usuário não autenticado");
        }
        return ResponseEntity.ok(UserResponseDTO.from(
                userService.updateProfile(user.getId(), request)));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request, @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            throw new IllegalStateException("Usuário não autenticado");
        }
        userService.changePassword(user.getId(), request);
        return ResponseEntity.noContent().build();
    }
}
