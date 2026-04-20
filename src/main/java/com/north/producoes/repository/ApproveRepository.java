package com.north.producoes.repository;

import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface ApproveRepository extends JpaRepository<ApproveEntity, Long> {
    List<ApproveEntity> findApproveEntitiesByStatus(ApproveStatusEnum status);

    List<ApproveEntity> findByPostId(Long id);

    void deleteByPostId(Long postId);
}
