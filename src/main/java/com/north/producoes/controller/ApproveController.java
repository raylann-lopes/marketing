package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.ApproveRequestDTO;
import com.north.producoes.controller.dto.response.ApproveResponseDTO;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.repository.ApproveRepository;
import com.north.producoes.service.ApprovedService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@AllArgsConstructor
@RequestMapping("/api/post-approvals")
public class ApproveController {

    private final ApproveRepository approveRepository;
    private final ApprovedService approvedService;

    @GetMapping("/all")
    public ResponseEntity<List<ApproveResponseDTO>> findAll() {
        List<ApproveEntity> approvals = approveRepository.findAll();
        return ResponseEntity.ok(approvals.
                stream()
                .map(ApproveResponseDTO::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApproveResponseDTO> findByPostId(@PathVariable Long id) {
        List<ApproveEntity> approvals = approveRepository.findByPostId(id);
        if (approvals.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApproveResponseDTO.from(approvals.getFirst()));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ApproveResponseDTO>> findApproveByStatus(@PathVariable ApproveStatusEnum status) {
        List<ApproveEntity> getApproveStatus = approveRepository.findApproveEntitiesByStatus(status);
        return ResponseEntity.ok(getApproveStatus.
                stream()
                .map(ApproveResponseDTO::from)
                .toList());
    }

    @PostMapping("/approve/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<ApproveResponseDTO> approvePost(@PathVariable Long id) {
        List<ApproveEntity> approvals = approveRepository.findByPostId(id);
        if (approvals.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ApproveEntity approve = approvals.getFirst();
        approve.setStatus(ApproveStatusEnum.APPROVE);
        return ResponseEntity.ok(ApproveResponseDTO.from(approveRepository.save(approve)));
    }

    @PostMapping("/reject/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<ApproveResponseDTO> rejectPost(@PathVariable Long id) {
        List<ApproveEntity> approvals = approveRepository.findByPostId(id);
        if (approvals.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ApproveEntity approve = approvals.getFirst();
        approve.setStatus(ApproveStatusEnum.REJECTED);
        return ResponseEntity.ok(ApproveResponseDTO.from(approveRepository.save(approve)));
    }

    @PostMapping("/save")
    public ResponseEntity<ApproveResponseDTO> saveApprove(@Valid @RequestBody ApproveRequestDTO approve) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApproveResponseDTO.from(approvedService.saveApprove(approve)));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApproveResponseDTO> updateApprove(@PathVariable Long id, @Valid @RequestBody ApproveRequestDTO approve) {
        return ResponseEntity.ok(ApproveResponseDTO.from(approvedService.updateApprove(id, approve)));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<Void> deleteApproveById(@PathVariable Long id) {
        approvedService.deleteApproveById(id);
        return ResponseEntity.noContent().build();
    }
}
