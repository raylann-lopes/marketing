package com.north.producoes.repository;

import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.UserRoleEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findFirstByRole(UserRoleEnum role);
}
