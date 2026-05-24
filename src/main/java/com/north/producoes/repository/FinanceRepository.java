package com.north.producoes.repository;

import com.north.producoes.entity.FinanceEntity;
import com.north.producoes.entity.enums.FinanceStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository

public interface FinanceRepository extends JpaRepository<FinanceEntity, Long> {
    List<FinanceEntity> findByStatus(FinanceStatusEnum status);
    List<FinanceEntity> findByClientId(Long id);
    List<FinanceEntity> findByClientIdAndStatusAndExpirationDateAfter(Long clientId, FinanceStatusEnum status, LocalDateTime date);
    void deleteByClientIdAndStatusAndExpirationDateAfter(Long clientId, FinanceStatusEnum status, LocalDateTime date);
    List<FinanceEntity> findByExpirationDateBetween(LocalDateTime start, LocalDateTime end);
    List<FinanceEntity> findByClientIdAndExpirationDateBetween(Long clientId, LocalDateTime start, LocalDateTime end);
}
