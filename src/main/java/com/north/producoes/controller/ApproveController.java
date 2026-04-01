package com.north.producoes.controller;

import com.north.producoes.controller.api.ApproveApi;
import com.north.producoes.controller.dto.response.ApproveResponse;
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
    public ResponseEntity<List<ApproveResponse>> findAll() {
        List<ApproveEntity> approvals = approveRepository.findAll();
        if (approvals.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(approvals.
                stream()
                .map(ApproveResponse::from)
                .toList());
    }

    @Override
    public ResponseEntity<ApproveResponse> findByPostId(Long id) {
        List<ApproveEntity> approvals = approveRepository.findByPostId(id);
        if (approvals.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApproveResponse.from(approvals.getFirst()));
    }

    @Override
    public ResponseEntity<List<ApproveResponse>> findApproveByStatus(ApproveStatusEnum status) {
        List<ApproveEntity> getApproveStatus = approveRepository.findApproveEntitiesByStatus(status);
        if (getApproveStatus.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(getApproveStatus.
                stream()
                .map(ApproveResponse::from)
                .toList());
    }
    @Override
    public ResponseEntity<ApproveResponse> approvePost(Long id) {
        List<ApproveEntity> approvals = approveRepository.findByPostId(id);
        if (approvals.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ApproveEntity approve = approvals.getFirst();
        approve.setStatus(ApproveStatusEnum.APPROVE);
        return ResponseEntity.ok(ApproveResponse.from(approveRepository.save(approve)));
    }

    @Override
    public ResponseEntity<ApproveResponse> rejectPost(Long id) {
        List<ApproveEntity> approvals = approveRepository.findByPostId(id);
        if (approvals.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ApproveEntity approve = approvals.getFirst();
        approve.setStatus(ApproveStatusEnum.REJECT);
        return ResponseEntity.ok(ApproveResponse.from(approveRepository.save(approve)));
    }

    @Override
    public ResponseEntity<ApproveResponse> saveApprove(ApproveEntity approve) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApproveResponse.from(approvedService.saveApprove(approve)));
    }

    @Override
    public ResponseEntity<ApproveResponse> updateApprove(Long id, ApproveEntity approve) {
        approve.setId(id);
        return ResponseEntity.ok(ApproveResponse.from(approvedService.updateApprove(approve)));
    }

    @Override
    public ResponseEntity<Void> deleteApproveById(Long id) {
        approvedService.deleteApproveById(id);
        return ResponseEntity.noContent().build();
    }
}
