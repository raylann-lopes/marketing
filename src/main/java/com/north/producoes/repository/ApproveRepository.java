package com.north.producoes.repository;

import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository

public interface ApproveRepository extends JpaRepository<ApproveEntity, Long> {
    List<ApproveEntity> findApproveEntitiesByStatus(ApproveStatusEnum status);

    List<ApproveEntity> findByPostId(Long id);

    @Query("SELECT a FROM ApproveEntity a WHERE a.whatsappStanzaId = ?1")
    Optional<ApproveEntity> findByWhatsappStanzaId(String whatsappStanzaId);

    void deleteByPostId(Long postId);
}
