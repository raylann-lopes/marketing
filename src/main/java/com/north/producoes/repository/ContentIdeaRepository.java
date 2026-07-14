package com.north.producoes.repository;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.ContentIdeaEntity;
import com.north.producoes.entity.enums.ContentIdeaStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ContentIdeaRepository extends JpaRepository<ContentIdeaEntity, Long> {
    List<ContentIdeaEntity> findByClient(ClientEntity client);

    List<ContentIdeaEntity> findByStatus(ContentIdeaStatusEnum status);

    // JOIN FETCH obrigatório: client é LAZY e open-in-view=false —
    // o mapeamento para DTO acessa client.getName() fora de sessão
    @Query("SELECT i FROM ContentIdeaEntity i JOIN FETCH i.client ORDER BY i.createdAt DESC")
    List<ContentIdeaEntity> findAllWithClient();

    @Query("SELECT i FROM ContentIdeaEntity i JOIN FETCH i.client WHERE i.id = :id")
    Optional<ContentIdeaEntity> findByIdWithClient(@Param("id") Long id);

    // Dedup: não recriar ideia com mesmo título para o cliente dentro da janela recente
    boolean existsByClientIdAndTitleIgnoreCaseAndCreatedAtAfter(
            Long clientId, String title, LocalDateTime after);
}
