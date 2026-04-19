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
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByNumberAndIdNot(String number, Long id);
    List<ClientEntity> findByStatus(ClientStatusEnum status);
}
