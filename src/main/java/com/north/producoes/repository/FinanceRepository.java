package com.north.producoes.repository;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.FinanceEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.FinanceStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceRepository extends JpaRepository<FinanceEntity, Long> {
    List<FinanceEntity> findByStatus(FinanceStatusEnum status);

    List<FinanceEntity> findByUserId(UserEntity user);

    List<FinanceEntity> findByClientId(ClientEntity clientId);
}
