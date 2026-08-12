package com.north.producoes.service;

import com.north.producoes.controller.dto.request.TaskRequestDTO;
import com.north.producoes.controller.dto.request.TaskStatusUpdateRequestDTO;
import com.north.producoes.controller.dto.response.TaskResponseDTO;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.TaskEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.TaskPriorityEnum;
import com.north.producoes.entity.enums.TaskSourceEnum;
import com.north.producoes.entity.enums.TaskStatusEnum;
import com.north.producoes.entity.enums.TaskTypeEnum;
import com.north.producoes.repository.ClientRepository;
import com.north.producoes.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public Page<TaskResponseDTO> listAllTask(
            UserEntity currentUser,
            TaskStatusEnum status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à data final");
        }

        Long userId = currentUser.getId();
        Page<TaskEntity> tasks;

        if (status != null && startDate != null && endDate != null) {
            tasks = taskRepository.findByUserIdAndStatusAndDateExpiresBetweenOrderByDateExpiresAscTimeExpiresAsc(
                    userId, status, startDate, endDate, pageable);
        } else if (status != null && startDate != null) {
            tasks = taskRepository.findByUserIdAndStatusAndDateExpiresGreaterThanEqualOrderByDateExpiresAscTimeExpiresAsc(
                    userId, status, startDate, pageable);
        } else if (status != null && endDate != null) {
            tasks = taskRepository.findByUserIdAndStatusAndDateExpiresLessThanEqualOrderByDateExpiresAscTimeExpiresAsc(
                    userId, status, endDate, pageable);
        } else if (status != null) {
            tasks = taskRepository.findByUserIdAndStatusOrderByDateExpiresAscTimeExpiresAsc(
                    userId, status, pageable);
        } else if (startDate != null && endDate != null) {
            tasks = taskRepository.findByUserIdAndDateExpiresBetweenOrderByDateExpiresAscTimeExpiresAsc(
                    userId, startDate, endDate, pageable);
        } else if (startDate != null) {
            tasks = taskRepository.findByUserIdAndDateExpiresGreaterThanEqualOrderByDateExpiresAscTimeExpiresAsc(
                    userId, startDate, pageable);
        } else if (endDate != null) {
            tasks = taskRepository.findByUserIdAndDateExpiresLessThanEqualOrderByDateExpiresAscTimeExpiresAsc(
                    userId, endDate, pageable);
        } else {
            tasks = taskRepository.findByUserIdOrderByDateExpiresAscTimeExpiresAsc(userId, pageable);
        }

        return tasks.map(TaskResponseDTO::from);
    }

    @Transactional
    public TaskResponseDTO createTask(TaskRequestDTO request, UserEntity currentUser) {
        return saveTask(request, currentUser, null);
    }

    @Transactional
    public TaskResponseDTO createWhatsAppTask(
            TaskRequestDTO request,
            UserEntity currentUser,
            String sourceReference) {
        return taskRepository.findBySourceReference(sourceReference)
                .map(TaskResponseDTO::from)
                .orElseGet(() -> saveTask(request, currentUser, sourceReference));
    }

    @Transactional
    public List<TaskResponseDTO> createWhatsAppTasks(
            List<TaskRequestDTO> requests,
            UserEntity currentUser,
            String messageId) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma tarefa informada para criação.");
        }

        return taskRepository.findBySourceReference(messageId)
                .map(existing -> List.of(TaskResponseDTO.from(existing)))
                .orElseGet(() -> java.util.stream.IntStream.range(0, requests.size())
                        .mapToObj(index -> saveTask(
                                requests.get(index),
                                currentUser,
                                index == 0 ? messageId : messageId + ":" + (index + 1)))
                        .toList());
    }

    @Transactional(readOnly = true)
    public Optional<TaskResponseDTO> findBySourceReference(String sourceReference) {
        if (sourceReference == null || sourceReference.isBlank()) return Optional.empty();
        return taskRepository.findBySourceReference(sourceReference).map(TaskResponseDTO::from);
    }

    private TaskResponseDTO saveTask(
            TaskRequestDTO request,
            UserEntity currentUser,
            String sourceReference) {
        TaskEntity task = new TaskEntity();
        task.setUser(currentUser);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDateExpires(request.dateExpires());
        task.setTimeExpires(request.timeExpires());
        task.setType(request.type() != null ? request.type() : TaskTypeEnum.TAREFA);
        task.setPriority(request.priority() != null ? request.priority() : TaskPriorityEnum.NORMAL);
        task.setStatus(request.status() != null ? request.status() : TaskStatusEnum.PENDING);
        task.setSource(request.source() != null ? request.source() : TaskSourceEnum.MANUAL);
        task.setSourceReference(sourceReference);
        task.setCreatedAt(LocalDateTime.now());

        if (request.clientId() != null) {
            ClientEntity client = clientRepository.findById(request.clientId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
            task.setClient(client);
            task.setClientName(client.getName());
        } else {
            task.setClientName(request.clientName());
        }

        TaskEntity savedTask = taskRepository.save(task);
        return TaskResponseDTO.from(savedTask);
    }

    @Transactional
    public TaskResponseDTO updateTask(Long taskId, TaskStatusUpdateRequestDTO request ) {
        var task = taskRepository.findById(taskId).orElseThrow(
                () -> new IllegalArgumentException("Tarefa nao encontrada")
        );
        if (request.status() == null) {
            throw new IllegalArgumentException("Status da tarefa é obrigatório");
        }
        task.setStatus(request.status());
        return TaskResponseDTO.from(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long taskId){
        var task = taskRepository.findById(taskId).orElseThrow(
                () -> new IllegalArgumentException("Tarefa nao encontrada")
        );
        taskRepository.delete(task);
    }

}
