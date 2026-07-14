package com.north.producoes.controller;

import com.north.producoes.controller.api.ApproveApi;
import com.north.producoes.controller.dto.request.ApproveByPostRequestDTO;
import com.north.producoes.controller.dto.request.ApproveRequestDTO;
import com.north.producoes.controller.dto.request.RejectByPostRequestDTO;
import com.north.producoes.controller.dto.response.ApproveResponseDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.service.ApprovedService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/post-approvals")
@PreAuthorize("isAuthenticated()")
public class ApproveController implements ApproveApi {

    private final ApprovedService approvedService;

    @Override
    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<List<ApproveResponseDTO>> findAll() {
        return ResponseEntity.ok(approvedService.findAll());
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<ApproveResponseDTO> findByPostId(
            @PathVariable Long id,
            @AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.ok(approvedService.findByPostId(id, currentUser));
    }

    @Override
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<List<ApproveResponseDTO>> findApproveByStatus(
            @PathVariable ApproveStatusEnum status) {
        return ResponseEntity.ok(approvedService.findByStatus(status));
    }

    @Override
    @PostMapping("/save")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<ApproveResponseDTO> saveApprove(
            @Valid @RequestBody ApproveRequestDTO approve) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(approvedService.saveApproveDTO(approve));
    }

    @Override
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<ApproveResponseDTO> updateApprove(
            @PathVariable Long id,
            @Valid @RequestBody ApproveRequestDTO approve) {
        return ResponseEntity.ok(approvedService.updateApproveDTO(id, approve));
    }

    @Override
    @PatchMapping("/approve/{postId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<ApproveResponseDTO> approveByPostId(
            @PathVariable Long postId,
            @RequestBody(required = false) ApproveByPostRequestDTO dto,
            @AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.ok(approvedService.approveByPostId(postId, currentUser.getEmail(), dto));
    }

    @Override
    @PatchMapping("/reject/{postId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<ApproveResponseDTO> rejectByPostId(
            @PathVariable Long postId,
            @RequestBody(required = false) RejectByPostRequestDTO dto,
            @AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.ok(approvedService.rejectByPostId(postId, currentUser.getEmail(), dto));
    }

    @Override
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<Void> deleteApproveById(@PathVariable Long id) {
        approvedService.deleteApproveById(id);
        return ResponseEntity.noContent().build();
    }
}
