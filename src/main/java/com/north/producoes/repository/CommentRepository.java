package com.north.producoes.repository;

import com.north.producoes.controller.dto.response.CommentCountResponseDTO;
import com.north.producoes.entity.CommentEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    @EntityGraph(attributePaths = {"user"})
    List<CommentEntity> findByPostIdOrderByCreatedAtAsc(Long postId);

    // Uma query só pro board inteiro — evita 1 request de contagem por card
    @Query("""
            SELECT new com.north.producoes.controller.dto.response.CommentCountResponseDTO(c.post.id, COUNT(c))
            FROM CommentEntity c
            GROUP BY c.post.id
            """)
    List<CommentCountResponseDTO> countGroupedByPost();

    // Sem ON DELETE CASCADE na FK — precisa limpar antes de apagar o post
    void deleteByPostId(Long postId);

    // Bulk delete JPQL vai direto ao banco e ignora o cascade do JPA — sem
    // esta limpeza prévia, excluir um cliente com posts comentados viola a FK
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CommentEntity c WHERE c.post.client.id = :clientId")
    void deleteByPostClientId(@Param("clientId") Long clientId);
}
