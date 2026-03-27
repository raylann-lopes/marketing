package com.north.producoes.repository;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<PostEntity, Long> {
    List<PostEntity> findByStatus(String status);
    List<PostEntity> findByClient(ClientEntity client);
    List<PostEntity> findByUser(UserEntity user);
    List<PostEntity> findByScheduledAtBetween(LocalDateTime scheduledAtAfter, LocalDateTime scheduledAtBefore);
}
