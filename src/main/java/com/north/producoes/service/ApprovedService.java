package com.north.producoes.service;

import com.north.producoes.controller.dto.request.ApproveRequestDTO;
import com.north.producoes.controller.dto.request.ApproveStatusUpdateRequestDTO;
import com.north.producoes.controller.dto.request.ApproveWhatsAppUpdateRequestDTO;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ApproveRepository;
import com.north.producoes.repository.PostRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class ApprovedService {

    private final ApproveRepository approveRepository;
    private final PostRepository postRepository;

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

    public ApproveEntity saveApprove(ApproveRequestDTO dto){
        PostEntity post = postRepository.findById(dto.postId())
                .orElseThrow(() -> new ResourceNotFoundException("Post não encontrado com id: " + dto.postId()));

        ApproveEntity approve = new ApproveEntity();
        approve.setPost(post);
        approve.setArtS3Key(dto.artS3Key());
        approve.setArtName(dto.artName());
        approve.setCaption(dto.caption());
        approve.setApprovedUser("");

        return approveRepository.save(approve);
    }

    public ApproveEntity findByStanzaId(String stanzaId ) {
        return approveRepository.findByWhatsappStanzaId(stanzaId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum registro de aprovação encontrado com WhatsApp Stanza ID: " + stanzaId));
    }


    public ApproveEntity updateApprove(Long id, ApproveRequestDTO dto){
        ApproveEntity existing = approveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum registro de aprovação encontrado com id: " + id));

        PostEntity post = postRepository.findById(dto.postId())
                .orElseThrow(() -> new ResourceNotFoundException("Post não encontrado com id: " + dto.postId()));

        existing.setPost(post);
        existing.setArtS3Key(dto.artS3Key());
        existing.setArtName(dto.artName());
        existing.setCaption(dto.caption());

        return approveRepository.save(existing);
    }

    public String getS3KeyByPostId(Long postId) {
        List<ApproveEntity> approvals = approveRepository.findByPostId(postId);
        if (approvals.isEmpty()) {
            throw new ResourceNotFoundException("Nenhuma aprovação encontrada para o post ID: " + postId);
        }
        return approvals.getFirst().getArtS3Key();
    }

    public void deleteApproveById(Long id){
        if (!approveRepository.existsById(id)) {
            throw new ResourceNotFoundException("Nenhum registro de aprovação encontrado com id: " + id);
        }
        approveRepository.deleteById(id);
    }


    public ApproveEntity updateWhatsappMetadata(Long id, ApproveWhatsAppUpdateRequestDTO dto) {
        ApproveEntity existing = approveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum registro de aprovação encontrado com id: " + id));

        existing.setWhatsappStanzaId(dto.whatsappStanzaId());
        existing.setWhatsappSentAt(dto.whatsappSentAt());
        existing.setWhatsappResponseText(dto.whatsappResponseText());

        return approveRepository.save(existing);
    }

    public ApproveEntity updateApprovalStatus(Long id, ApproveStatusUpdateRequestDTO dto) {
        ApproveEntity existing = approveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum registro de aprovação encontrado com id: " + id));

        if (dto.status() == ApproveStatusEnum.PENDING) {
            throw new IllegalArgumentException("Endpoint interno aceita apenas status APPROVE ou REJECT");
        }

        existing.setStatus(dto.status());
        if (dto.status() == ApproveStatusEnum.APPROVE) {
            existing.setApprovedAt(LocalDateTime.now());
            existing.setApprovedUser("n8n-callback");
        } else {
            existing.setApprovedAt(null);
            existing.setApprovedUser("");
        }

        return approveRepository.save(existing);
    }
}
