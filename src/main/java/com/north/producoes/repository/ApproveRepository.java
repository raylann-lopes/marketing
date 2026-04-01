package com.north.producoes.repository;

import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApproveRepository extends JpaRepository<ApproveEntity, Long> {
    List<ApproveEntity> findApproveEntitiesByStatus(ApproveStatusEnum status);

    List<ApproveEntity> findByPostId(Long id);
}
