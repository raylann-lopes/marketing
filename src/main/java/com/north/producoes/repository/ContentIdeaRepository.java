package com.north.producoes.repository;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.ContentIdeaEntity;
import com.north.producoes.entity.enums.ContentIdeaStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentIdeaRepository extends JpaRepository<ContentIdeaEntity, Long> {
    List<ContentIdeaEntity> findByClient(ClientEntity client);

    List<ContentIdeaEntity> findByStatus(ContentIdeaStatusEnum status);

}
