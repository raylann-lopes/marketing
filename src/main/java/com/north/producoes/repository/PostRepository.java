package com.north.producoes.repository;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.PostStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<PostEntity, Long> {
    List<PostEntity> findByStatus(PostStatusEnum status);
    List<PostEntity> findByClient(ClientEntity client);
    List<PostEntity> findByUser(UserEntity user);
    List<PostEntity> findByScheduledAtBetween(LocalDateTime scheduledAtAfter, LocalDateTime scheduledAtBefore);
}
