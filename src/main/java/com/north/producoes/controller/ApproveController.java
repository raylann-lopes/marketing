package com.north.producoes.controller;

import com.north.producoes.controller.api.ApproveApi;
import com.north.producoes.controller.dto.response.ApproveResponseDTO;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.repository.ApproveRepository;
import com.north.producoes.service.ApprovedService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class ApproveController implements ApproveApi {

    private final ApproveRepository approveRepository;
    private final ApprovedService approvedService;

    @Override
    public ResponseEntity<List<ApproveResponseDTO>> findAll() {
        List<ApproveEntity> approvals = approveRepository.findAll();
        if (approvals.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(approvals.
                stream()
                .map(ApproveResponseDTO::from)
                .toList());
    }

    @Override
    public ResponseEntity<ApproveResponseDTO> findByPostId(Long id) {
        List<ApproveEntity> approvals = approveRepository.findByPostId(id);
        if (approvals.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApproveResponseDTO.from(approvals.getFirst()));
    }

    @Override
    public ResponseEntity<List<ApproveResponseDTO>> findApproveByStatus(ApproveStatusEnum status) {
        List<ApproveEntity> getApproveStatus = approveRepository.findApproveEntitiesByStatus(status);
        if (getApproveStatus.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(getApproveStatus.
                stream()
                .map(ApproveResponseDTO::from)
                .toList());
    }
    @Override
    public ResponseEntity<ApproveResponseDTO> approvePost(Long id) {
        List<ApproveEntity> approvals = approveRepository.findByPostId(id);
        if (approvals.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ApproveEntity approve = approvals.getFirst();
        approve.setStatus(ApproveStatusEnum.APPROVE);
        return ResponseEntity.ok(ApproveResponseDTO.from(approveRepository.save(approve)));
    }

    @Override
    public ResponseEntity<ApproveResponseDTO> rejectPost(Long id) {
        List<ApproveEntity> approvals = approveRepository.findByPostId(id);
        if (approvals.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ApproveEntity approve = approvals.getFirst();
        approve.setStatus(ApproveStatusEnum.REJECT);
        return ResponseEntity.ok(ApproveResponseDTO.from(approveRepository.save(approve)));
    }

    @Override
    public ResponseEntity<ApproveResponseDTO> saveApprove(ApproveEntity approve) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApproveResponseDTO.from(approvedService.saveApprove(approve)));
    }

    @Override
    public ResponseEntity<ApproveResponseDTO> updateApprove(Long id, ApproveEntity approve) {
        approve.setId(id);
        return ResponseEntity.ok(ApproveResponseDTO.from(approvedService.updateApprove(approve)));
    }

    @Override
    public ResponseEntity<Void> deleteApproveById(Long id) {
        approvedService.deleteApproveById(id);
        return ResponseEntity.noContent().build();
    }
}
