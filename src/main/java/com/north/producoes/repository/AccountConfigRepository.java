package com.north.producoes.repository;

import com.north.producoes.entity.AccountConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountConfigRepository extends JpaRepository<AccountConfigEntity, Long> {
    Optional<AccountConfigEntity> findByClientId(Long clientId);
    boolean existsByClientId(Long clientId);
}
