package com.north.producoes.controller;

import com.north.producoes.entity.dto.request.UserRequest;
import com.north.producoes.entity.dto.response.UserResponse;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> findAll(){
        List<UserResponse> user = userService.findAllUser()
                .stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(user);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> findUserByEmail(@PathVariable String email){
        return ResponseEntity.ok(UserResponse.from(userService.findUserByEmail(email)));
    }

    @PostMapping
    public ResponseEntity<UserResponse> saveUser(@Valid @RequestBody UserRequest request){
        UserEntity user = new UserEntity();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponse.from(userService.saveUser(user)));
    }

    @DeleteMapping("/id/{id}")
    public ResponseEntity<Void> deleteUserById(@PathVariable Long id){
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

}
