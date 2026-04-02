package com.north.producoes.service;

import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ApproveRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ApprovedService {
    private final ApproveRepository approveRepository;

    public Optional<ApproveEntity> findById(Long id) {
        if (!approveRepository.existsById(id)) {
            throw new ResourceNotFoundException("Nenhum post encontrado com id: " + id);
        }
            return approveRepository.findById(id);
    }

    public List<ApproveEntity> findApproveByStatus(ApproveStatusEnum status){
        List<ApproveEntity> approveStatus = approveRepository.findApproveEntitiesByStatus(status);
        if (approveStatus.isEmpty()){
            throw new ResourceNotFoundException("Nenhum post encontrado com status: " + status);
        }
        return approveStatus;
    }

    public ApproveEntity saveApprove(ApproveEntity approve){
        return approveRepository.save(approve);
    }

    public ApproveEntity updateApprove(ApproveEntity approve){
        if (!approveRepository.existsById(approve.getId())){
            throw new ResourceNotFoundException("Nenhum registro de aprovação encontrado com id: " + approve.getId());
        }
        return approveRepository.save(approve);
    }

    public void deleteApproveById(Long id){
        if (!approveRepository.existsById(id)) {
            throw new ResourceNotFoundException("Nenhum registro de aprovação encontrado com id: " + id);
        }
        approveRepository.deleteById(id);
    }
}
