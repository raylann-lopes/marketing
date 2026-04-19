package com.north.producoes.repository;

import com.north.producoes.entity.FinanceEntity;
import com.north.producoes.entity.enums.FinanceStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface FinanceRepository extends JpaRepository<FinanceEntity, Long> {
    List<FinanceEntity> findByStatus(FinanceStatusEnum status);
    List<FinanceEntity> findByClientId(Long id);
}
