package com.north.producoes.repository;

import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.enums.PostStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<PostEntity, Long> {
    List<PostEntity> findByStatus(PostStatusEnum status);
    List<PostEntity> findByClientId(Long id);
    List<PostEntity> findByUserId(Long id);
    List<PostEntity> findByScheduledAtBetween(LocalDateTime scheduledAtAfter, LocalDateTime scheduledAtBefore);
}
