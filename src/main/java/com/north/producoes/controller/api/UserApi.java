package com.north.producoes.controller.api;

import com.north.producoes.controller.dto.request.UserRequest;
import com.north.producoes.controller.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Usuários", description = "Gerenciamento de usuários do sistema")
@RequestMapping("/api/users")
public interface UserApi {

    @Operation(summary = "Lista todos os usuários")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    ResponseEntity<List<UserResponse>> findAll();

    @Operation(summary = "Busca usuário por email")
    @ApiResponse(responseCode = "200", description = "Usuário encontrado")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @GetMapping("/email/{email}")
    ResponseEntity<UserResponse> findUserByEmail(
            @Parameter(description = "Email do usuário") @PathVariable String email);

    @Operation(summary = "Cadastra um novo usuário")
    @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso")
    @ApiResponse(responseCode = "409", description = "Usuário já cadastrado")
    @PostMapping
    ResponseEntity<UserResponse> saveUser(@Valid @RequestBody UserRequest request);

    @Operation(summary = "Remove um usuário pelo ID")
    @ApiResponse(responseCode = "204", description = "Usuário removido com sucesso")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @DeleteMapping("/id/{id}")
    ResponseEntity<Void> deleteUserById(
            @Parameter(description = "ID do usuário") @PathVariable Long id);
}
