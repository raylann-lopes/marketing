package com.north.producoes.repository;

import com.north.producoes.entity.AccountConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository

public interface AccountConfigRepository extends JpaRepository<AccountConfigEntity, Long> {
    Optional<AccountConfigEntity> findByClientId(Long clientId);
    Optional<AccountConfigEntity> findByIgUserId(String igUserId);
    boolean existsByClientId(Long clientId);

    void deleteByClientId(Long clientId);
}
