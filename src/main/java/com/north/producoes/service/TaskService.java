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
import com.north.producoes.exception.ResourceNotFoundException;
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
        return saveTask(request, currentUser, null, TaskSourceEnum.MANUAL);
    }

    @Transactional
    public List<TaskResponseDTO> createWhatsAppTasks(
            List<TaskRequestDTO> requests,
            UserEntity currentUser,
            String messageId) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma tarefa informada para criação.");
        }

        List<TaskEntity> existingTasks = findAllBySourceMessageIdEntities(messageId);
        if (!existingTasks.isEmpty()) {
            return existingTasks.stream().map(TaskResponseDTO::from).toList();
        }

        return java.util.stream.IntStream.range(0, requests.size())
                .mapToObj(index -> saveTask(
                        requests.get(index),
                        currentUser,
                        index == 0 ? messageId : messageId + ":" + (index + 1),
                        TaskSourceEnum.WHATSAPP))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDTO> findAllBySourceMessageId(String messageId) {
        if (messageId == null || messageId.isBlank()) return List.of();
        return findAllBySourceMessageIdEntities(messageId).stream()
                .map(TaskResponseDTO::from)
                .toList();
    }

    private TaskResponseDTO saveTask(
            TaskRequestDTO request,
            UserEntity currentUser,
            String sourceReference,
            TaskSourceEnum source) {
        TaskEntity task = new TaskEntity();
        task.setUser(currentUser);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDateExpires(request.dateExpires());
        task.setTimeExpires(request.timeExpires());
        task.setType(request.type() != null ? request.type() : TaskTypeEnum.TAREFA);
        task.setPriority(request.priority() != null ? request.priority() : TaskPriorityEnum.NORMAL);
        task.setStatus(TaskStatusEnum.PENDING);
        task.setSource(source);
        task.setSourceReference(sourceReference);
        task.setCreatedAt(LocalDateTime.now());

        if (request.clientId() != null) {
            ClientEntity client = clientRepository.findById(request.clientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
            task.setClient(client);
            task.setClientName(client.getName());
        } else {
            task.setClientName(request.clientName());
        }

        TaskEntity savedTask = taskRepository.save(task);
        return TaskResponseDTO.from(savedTask);
    }

    @Transactional
    public TaskResponseDTO updateTask(
            Long taskId,
            TaskStatusUpdateRequestDTO request,
            UserEntity currentUser) {
        var task = taskRepository.findByIdAndUserId(taskId, currentUser.getId()).orElseThrow(
                () -> new ResourceNotFoundException("Tarefa não encontrada")
        );
        if (request.status() == null) {
            throw new IllegalArgumentException("Status da tarefa é obrigatório");
        }
        task.setStatus(request.status());
        return TaskResponseDTO.from(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long taskId, UserEntity currentUser){
        var task = taskRepository.findByIdAndUserId(taskId, currentUser.getId()).orElseThrow(
                () -> new ResourceNotFoundException("Tarefa não encontrada")
        );
        taskRepository.delete(task);
    }

    private List<TaskEntity> findAllBySourceMessageIdEntities(String messageId) {
        return taskRepository.findBySourceReferenceOrSourceReferenceStartingWithOrderByIdAsc(
                messageId,
                messageId + ":");
    }

}
