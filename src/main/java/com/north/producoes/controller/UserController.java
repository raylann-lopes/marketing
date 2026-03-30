package com.north.producoes.controller;

import com.north.producoes.controller.api.UserApi;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.controller.dto.request.UserRequest;
import com.north.producoes.controller.dto.response.UserResponse;
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
    public ResponseEntity<List<UserResponse>> findAll() {
        List<UserResponse> user = userService.findAllUser()
                .stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(user);
    }

    @Override
    public ResponseEntity<UserResponse> findUserByEmail(String email) {
        return ResponseEntity.ok(UserResponse.from(userService.findUserByEmail(email).orElseThrow()));
    }

    @Override
    public ResponseEntity<UserResponse> saveUser(UserRequest request) {
        UserEntity user = new UserEntity();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponse.from(userService.saveUser(user)));
    }

    @Override
    public ResponseEntity<Void> deleteUserById(Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }
}
