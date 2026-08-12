package com.north.producoes.repository;

import com.north.producoes.entity.TaskEntity;
import com.north.producoes.entity.enums.TaskStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    List<TaskEntity> findByUserIdOrderByDateExpiresAscTimeExpiresAsc(Long userId);

    List<TaskEntity> findByUserIdAndDateExpiresOrderByTimeExpiresAsc(Long userId, LocalDate date);

    List<TaskEntity> findByUserIdAndDateExpiresBetweenOrderByDateExpiresAscTimeExpiresAsc(Long userId, LocalDate startDate, LocalDate endDate);

    List<TaskEntity> findByUserIdAndStatusOrderByDateExpiresAscTimeExpiresAsc(Long userId, TaskStatusEnum status);

    List<TaskEntity> findByClientIdOrderByDateExpiresAscTimeExpiresAsc(Long clientId);

    Page<TaskEntity> findByUserIdOrderByDateExpiresAscTimeExpiresAsc(Long userId, Pageable pageable);

    Page<TaskEntity> findByUserIdAndStatusOrderByDateExpiresAscTimeExpiresAsc(
            Long userId,
            TaskStatusEnum status,
            Pageable pageable);

    Page<TaskEntity> findByUserIdAndDateExpiresOrderByTimeExpiresAsc(
            Long userId,
            LocalDate date,
            Pageable pageable);

    Page<TaskEntity> findByUserIdAndDateExpiresBetweenOrderByDateExpiresAscTimeExpiresAsc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable);

    Page<TaskEntity> findByUserIdAndDateExpiresGreaterThanEqualOrderByDateExpiresAscTimeExpiresAsc(
            Long userId,
            LocalDate startDate,
            Pageable pageable);

    Page<TaskEntity> findByUserIdAndDateExpiresLessThanEqualOrderByDateExpiresAscTimeExpiresAsc(
            Long userId,
            LocalDate endDate,
            Pageable pageable);

    Page<TaskEntity> findByUserIdAndStatusAndDateExpiresOrderByTimeExpiresAsc(
            Long userId,
            TaskStatusEnum status,
            LocalDate date,
            Pageable pageable);

    Page<TaskEntity> findByUserIdAndStatusAndDateExpiresBetweenOrderByDateExpiresAscTimeExpiresAsc(
            Long userId,
            TaskStatusEnum status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable);

    Page<TaskEntity> findByUserIdAndStatusAndDateExpiresGreaterThanEqualOrderByDateExpiresAscTimeExpiresAsc(
            Long userId,
            TaskStatusEnum status,
            LocalDate startDate,
            Pageable pageable);

    Page<TaskEntity> findByUserIdAndStatusAndDateExpiresLessThanEqualOrderByDateExpiresAscTimeExpiresAsc(
            Long userId,
            TaskStatusEnum status,
            LocalDate endDate,
            Pageable pageable);

    Optional<TaskEntity> findBySourceReference(String sourceReference);

}
