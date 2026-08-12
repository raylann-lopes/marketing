package com.north.producoes.repository;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.enums.ClientStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository

public interface ClientRepository extends JpaRepository<ClientEntity, Long> {
    Optional<ClientEntity> findByEmail(String email);
    Optional<ClientEntity> findByNumber(String number);
    Optional<ClientEntity> findByWhatsappGroupId(String whatsappGroupId);
    Optional<ClientEntity> findFirstByNameIgnoreCase(String name);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByNumberAndIdNot(String number, Long id);
    List<ClientEntity> findByStatus(ClientStatusEnum status);

    /** Substitui findAll().stream().filter(monthlyValue > 0) — filtra no banco, não na JVM */
    List<ClientEntity> findByMonthlyValueGreaterThan(java.math.BigDecimal value);
}
