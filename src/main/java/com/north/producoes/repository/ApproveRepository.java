package com.north.producoes.repository;

import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApproveRepository extends JpaRepository<ApproveEntity, Long> {

    // carouselArts é EAGER — o @EntityGraph traz a coleção no mesmo select
    // e evita uma query extra por aprovação (N+1) nas listagens do board
    @Override
    @EntityGraph(attributePaths = {"carouselArts"})
    List<ApproveEntity> findAll();

    @EntityGraph(attributePaths = {"carouselArts"})
    List<ApproveEntity> findApproveEntitiesByStatus(ApproveStatusEnum status);

    @EntityGraph(attributePaths = {"carouselArts"})
    List<ApproveEntity> findByPostId(Long id);

    @Query("SELECT a FROM ApproveEntity a WHERE a.whatsappStanzaId = ?1")
    Optional<ApproveEntity> findByWhatsappStanzaId(String whatsappStanzaId);

    // flush/clear automáticos: bulk delete vai direto ao banco e deixaria o
    // persistence context com entidades órfãs — o clear evita flush de entidades
    // já apagadas mais adiante na mesma transação (ex.: postRepository.delete(post)
    // logo em seguida, que veria post.approve como referência a instância removida)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ApproveEntity a WHERE a.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    // flush/clear automáticos: bulk delete vai direto ao banco e deixaria o
    // persistence context com entidades órfãs — o clear evita flush de entidades
    // já apagadas mais adiante na mesma transação
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ApproveEntity a WHERE a.post.client.id = :clientId")
    void deleteByPostClientId(@Param("clientId") Long clientId);

    /**
     * Remove as artes de carrossel antes do bulk delete das aprovações.
     * Bulk delete JPQL vai direto ao banco e ignora o cascade do JPA — sem
     * esta limpeza prévia, bancos sem ON DELETE CASCADE violam a FK.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM ApproveCarouselArtEntity a
            WHERE a.approve.id IN (
                SELECT ap.id FROM ApproveEntity ap WHERE ap.post.client.id = :clientId)
            """)
    void deleteCarouselArtsByPostClientId(@Param("clientId") Long clientId);

    /**
     * Remove as artes de carrossel antes do bulk delete da aprovação de um post.
     * Mesma razão de deleteCarouselArtsByPostClientId: bulk delete ignora o
     * cascade do JPA.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM ApproveCarouselArtEntity a
            WHERE a.approve.id IN (
                SELECT ap.id FROM ApproveEntity ap WHERE ap.post.id = :postId)
            """)
    void deleteCarouselArtsByPostId(@Param("postId") Long postId);

    /**
     * Busca aprovação PENDING vinculada ao grupo WhatsApp do cliente.
     * Usado pelo webhook para identificar a qual post o cliente está respondendo.
     * Retorna List (não Optional) para evitar IncorrectResultSizeDataAccessException
     * quando existem múltiplas aprovações PENDING para o mesmo grupo.
     * O chamador usa stream().findFirst().
     */
    @Query("""
            SELECT a FROM ApproveEntity a
            JOIN a.post p
            JOIN p.client c
            WHERE a.status = :status
              AND (c.whatsappGroupId = :groupId
                   OR c.whatsappGroupId = :groupIdWithSuffix)
            ORDER BY p.scheduledAt ASC
            """)
    List<ApproveEntity> findByStatusAndClientWhatsappGroupId(
            @Param("status") ApproveStatusEnum status,
            @Param("groupId") String groupId,
            @Param("groupIdWithSuffix") String groupIdWithSuffix);
}
