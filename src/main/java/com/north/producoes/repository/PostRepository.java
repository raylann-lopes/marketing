package com.north.producoes.repository;

import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.enums.PostStatusEnum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"carouselImages"})
    List<PostEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"carouselImages"})
    Optional<PostEntity> findById(Long id);

    @EntityGraph(attributePaths = {"carouselImages"})
    List<PostEntity> findByStatus(PostStatusEnum status);

    @EntityGraph(attributePaths = {"carouselImages"})
    List<PostEntity> findByClientId(Long id);

    @EntityGraph(attributePaths = {"carouselImages"})
    List<PostEntity> findByUserId(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PostEntity p WHERE p.client.id = :clientId")
    void deleteByClientId(@Param("clientId") Long clientId);

    /**
     * Remove as imagens de referência antes do bulk delete dos posts.
     * Bulk delete JPQL vai direto ao banco e ignora o cascade do JPA — sem
     * esta limpeza prévia, bancos sem ON DELETE CASCADE violam a FK.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM PostCarouselImageEntity i
            WHERE i.post.id IN (
                SELECT p.id FROM PostEntity p WHERE p.client.id = :clientId)
            """)
    void deleteCarouselImagesByClientId(@Param("clientId") Long clientId);

    @EntityGraph(attributePaths = {"carouselImages"})
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
     * @Transactional próprio: também é chamado fora de transação (publishAsync).
     */
    @Transactional
    @Modifying
    @Query("""
            UPDATE PostEntity p SET p.status = :newStatus
            WHERE p.id = :id AND p.status = :expectedStatus
            """)
    int compareAndSetStatus(@Param("id") Long id,
                            @Param("expectedStatus") PostStatusEnum expectedStatus,
                            @Param("newStatus") PostStatusEnum newStatus);
}
