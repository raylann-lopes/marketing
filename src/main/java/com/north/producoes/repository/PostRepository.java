package com.north.producoes.repository;

import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.enums.PostStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, Long> {

    List<PostEntity> findByStatus(PostStatusEnum status);

    List<PostEntity> findByClientId(Long id);

    List<PostEntity> findByUserId(Long id);

    List<PostEntity> findByScheduledAtBetween(LocalDateTime scheduledAtAfter,
                                               LocalDateTime scheduledAtBefore);

    boolean existsByIdAndUserId(Long id, Long userId);

    /**
     * Busca posts agendados já com approve carregado — elimina N+1 no scheduler.
     */
    @Query("""
            SELECT p FROM PostEntity p
            LEFT JOIN FETCH p.approve a
            LEFT JOIN FETCH p.client c
            WHERE p.status = :status
            """)
    List<PostEntity> findByStatusWithApproval(@Param("status") PostStatusEnum status);

    /**
     * Transição atômica de status — previne race condition em deploy multi-instância.
     * Retorna 1 se a atualização ocorreu, 0 se outro processo já mudou o status.
     */
    @Modifying
    @Query("""
            UPDATE PostEntity p SET p.status = :newStatus
            WHERE p.id = :id AND p.status = :expectedStatus
            """)
    int compareAndSetStatus(@Param("id") Long id,
                            @Param("expectedStatus") PostStatusEnum expectedStatus,
                            @Param("newStatus") PostStatusEnum newStatus);
}
