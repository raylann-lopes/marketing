package com.north.producoes.repository;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.ContentIdeaRunsEntity;
import com.north.producoes.entity.enums.ContentIdeaRunsStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentIdeaRunsRepository extends JpaRepository<ContentIdeaRunsEntity, Long> {
    List<ContentIdeaRunsEntity> findByClient(ClientEntity client);

    List<ContentIdeaRunsEntity> findByStatus(ContentIdeaRunsStatusEnum status);
}
