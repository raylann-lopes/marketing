package com.north.producoes.controller.api;

import com.north.producoes.controller.dto.request.AdminUserUpdateRequestDTO;
import com.north.producoes.controller.dto.request.ChangePasswordRequestDTO;
import com.north.producoes.controller.dto.request.UpdateRoleRequestDTO;
import com.north.producoes.controller.dto.request.UserRequestDTO;
import com.north.producoes.controller.dto.response.UserResponseDTO;
import com.north.producoes.entity.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Usuários", description = "Gerenciamento de usuários do sistema")
public interface UserApi {

    @GetMapping
    @Operation(summary = "Lista todos os usuários com paginação (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    ResponseEntity<List<UserResponseDTO>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size);

    @GetMapping("/email/{email}")
    @Operation(summary = "Busca usuário por email (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Usuário encontrado")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    ResponseEntity<UserResponseDTO> findUserByEmail(
            @Parameter(description = "Email do usuário") @PathVariable String email);

    @PostMapping
    @Operation(summary = "Cadastra um novo usuário (somente ADMIN)")
    @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso")
    @ApiResponse(responseCode = "409", description = "Usuário já cadastrado")
    ResponseEntity<UserResponseDTO> saveUser(@Valid @RequestBody UserRequestDTO request);

    @PatchMapping("/id/{id}/role")
    @Operation(summary = "Atualiza o papel de um usuário (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Papel atualizado com sucesso")
    @ApiResponse(responseCode = "403", description = "Admin não pode alterar o próprio papel")
    ResponseEntity<UserResponseDTO> updateRole(
            @Parameter(description = "ID do usuário") @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserEntity currentUser);

    @PutMapping("/id/{id}")
    @Operation(summary = "Edição administrativa de usuário (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    ResponseEntity<UserResponseDTO> adminUpdateUser(
            @Parameter(description = "ID do usuário") @PathVariable Long id,
            @Valid @RequestBody AdminUserUpdateRequestDTO request);

    @PatchMapping("/id/{id}/activate")
    @Operation(summary = "Reativa um usuário desativado (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Usuário reativado com sucesso")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    ResponseEntity<UserResponseDTO> activateUserById(
            @Parameter(description = "ID do usuário") @PathVariable Long id);

    @DeleteMapping("/id/{id}")
    @Operation(summary = "Desativa um usuário (soft delete, somente ADMIN)")
    @ApiResponse(responseCode = "204", description = "Usuário desativado com sucesso")
    @ApiResponse(responseCode = "403", description = "Admin não pode desativar a si mesmo")
    ResponseEntity<Void> deactivateUserById(
            @Parameter(description = "ID do usuário") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserEntity currentUser);

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário autenticado")
    @ApiResponse(responseCode = "200", description = "Usuário autenticado")
    ResponseEntity<UserResponseDTO> findMe(
            @Parameter(hidden = true) @AuthenticationPrincipal UserEntity user);

    @PutMapping("/me")
    @Operation(summary = "Atualiza perfil do usuário autenticado")
    @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso")
    ResponseEntity<UserResponseDTO> updateMe(
            @Valid @RequestBody UserRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserEntity user);

    @PutMapping("/me/password")
    @Operation(summary = "Altera senha do usuário autenticado")
    @ApiResponse(responseCode = "204", description = "Senha alterada com sucesso")
    ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserEntity user);
}
