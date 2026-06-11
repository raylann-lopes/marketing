package com.north.producoes.repository;

import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApproveRepository extends JpaRepository<ApproveEntity, Long> {

    List<ApproveEntity> findApproveEntitiesByStatus(ApproveStatusEnum status);

    List<ApproveEntity> findByPostId(Long id);

    @Query("SELECT a FROM ApproveEntity a WHERE a.whatsappStanzaId = ?1")
    Optional<ApproveEntity> findByWhatsappStanzaId(String whatsappStanzaId);

    void deleteByPostId(Long postId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM ApproveEntity a WHERE a.post.client.id = :clientId")
    void deleteByPostClientId(@org.springframework.data.repository.query.Param("clientId") Long clientId);

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
