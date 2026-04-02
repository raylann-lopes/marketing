package com.north.producoes.controller;

import com.north.producoes.controller.api.UserApi;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.controller.dto.request.UserRequestDTO;
import com.north.producoes.controller.dto.response.UserResponseDTO;
import com.north.producoes.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    @Override
    public ResponseEntity<List<UserResponseDTO>> findAll() {
        List<UserResponseDTO> user = userService.findAllUser()
                .stream()
                .map(UserResponseDTO::from)
                .toList();
        return ResponseEntity.ok(user);
    }

    @Override
    public ResponseEntity<UserResponseDTO> findUserByEmail(String email) {
        return ResponseEntity.ok(UserResponseDTO.from(userService.findUserByEmail(email).orElseThrow()));
    }

    @Override
    public ResponseEntity<UserResponseDTO> saveUser(UserRequestDTO request) {
        UserEntity user = new UserEntity();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponseDTO.from(userService.saveUser(user)));
    }

    @Override
    public ResponseEntity<Void> deleteUserById(Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }
}
