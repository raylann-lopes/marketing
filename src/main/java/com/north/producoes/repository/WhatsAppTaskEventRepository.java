package com.north.producoes.repository;

import com.north.producoes.entity.WhatsAppTaskEventEntity;
import com.north.producoes.entity.enums.WhatsAppTaskEventStatusEnum;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WhatsAppTaskEventRepository extends JpaRepository<WhatsAppTaskEventEntity, Long> {

    Optional<WhatsAppTaskEventEntity> findByMessageId(String messageId);

    @Modifying
    @Query(value = """
            INSERT INTO tb_whatsapp_task_event (message_id, payload, status, attempts, created_at, updated_at)
            VALUES (:messageId, :payload, 'PENDING', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (message_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("messageId") String messageId, @Param("payload") String payload);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE tb_whatsapp_task_event
               SET status = 'PROCESSING',
                   attempts = attempts + 1,
                   next_attempt_at = NULL,
                   last_error = NULL,
                   updated_at = CURRENT_TIMESTAMP
             WHERE id = :eventId
               AND attempts < :maxAttempts
               AND (
                    (status IN ('PENDING', 'FAILED')
                     AND (next_attempt_at IS NULL OR next_attempt_at <= :now))
                    OR (status = 'PROCESSING' AND updated_at < :staleBefore)
               )
            """, nativeQuery = true)
    int reserveForProcessing(
            @Param("eventId") Long eventId,
            @Param("maxAttempts") int maxAttempts,
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore);

    @Query("""
            SELECT event.id
              FROM WhatsAppTaskEventEntity event
             WHERE event.attempts < :maxAttempts
               AND (
                    (event.status IN :retryableStatuses
                     AND (event.nextAttemptAt IS NULL OR event.nextAttemptAt <= :now))
                    OR (event.status = :processingStatus AND event.updatedAt < :staleBefore)
               )
             ORDER BY event.createdAt ASC
            """)
    List<Long> findProcessableIds(
            @Param("retryableStatuses") List<WhatsAppTaskEventStatusEnum> retryableStatuses,
            @Param("processingStatus") WhatsAppTaskEventStatusEnum processingStatus,
            @Param("maxAttempts") int maxAttempts,
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore,
            Pageable pageable);
}
