package com.north.producoes.repository;

import com.north.producoes.entity.FinanceEntity;
import com.north.producoes.entity.enums.FinanceStatusEnum;
import com.north.producoes.entity.enums.FinanceTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface FinanceRepository extends JpaRepository<FinanceEntity, Long> {

    List<FinanceEntity> findByStatus(FinanceStatusEnum status);

    List<FinanceEntity> findByClientId(Long id);

    List<FinanceEntity> findByClientIdAndStatus(Long clientId, FinanceStatusEnum status);

    List<FinanceEntity> findByClientIdAndStatusAndExpirationDateAfter(
            Long clientId, FinanceStatusEnum status, LocalDateTime date);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM FinanceEntity f WHERE f.client.id = :clientId")
    void deleteByClientId(@Param("clientId") Long clientId);

    @Modifying
    @Query("DELETE FROM FinanceEntity f WHERE f.client.id = :clientId AND f.status = :status AND f.expirationDate > :date")
    void deleteByClientIdAndStatusAndExpirationDateAfter(
            @Param("clientId") Long clientId,
            @Param("status") FinanceStatusEnum status,
            @Param("date") LocalDateTime date);

    @Query("SELECT f FROM FinanceEntity f JOIN FETCH f.client WHERE f.expirationDate BETWEEN :start AND :end")
    List<FinanceEntity> findByExpirationDateBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT f FROM FinanceEntity f JOIN FETCH f.client WHERE f.client.id = :clientId AND f.expirationDate BETWEEN :start AND :end")
    List<FinanceEntity> findByClientIdAndExpirationDateBetween(
            @Param("clientId") Long clientId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    List<FinanceEntity> findByType(FinanceTypeEnum type);

    @Query("SELECT f.client.id FROM FinanceEntity f WHERE f.expirationDate BETWEEN :start AND :end")
    Set<Long> findClientIdsWithEntryInMonth(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Listagem com filtros opcionais para a tela de financeiro. O intervalo é
     * sempre aplicado (o service usa limites sentinela quando não informado)
     * para evitar parâmetros null de data no JPQL.
     */
    @Query("""
            SELECT f FROM FinanceEntity f JOIN FETCH f.client
            WHERE (:status IS NULL OR f.status = :status)
              AND f.expirationDate BETWEEN :start AND :end
            ORDER BY f.expirationDate ASC
            """)
    List<FinanceEntity> findFiltered(
            @Param("status") FinanceStatusEnum status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
