package com.north.producoes.service;

import com.north.producoes.controller.dto.request.TaskRequestDTO;
import com.north.producoes.controller.dto.request.TaskStatusUpdateRequestDTO;
import com.north.producoes.controller.dto.response.TaskResponseDTO;
import com.north.producoes.entity.TaskEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.TaskPriorityEnum;
import com.north.producoes.entity.enums.TaskSourceEnum;
import com.north.producoes.entity.enums.TaskStatusEnum;
import com.north.producoes.entity.enums.TaskTypeEnum;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ClientRepository;
import com.north.producoes.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void shouldListTasksUsingStatusAndDateFilters() {
        UserEntity owner = new UserEntity();
        owner.setId(1L);
        LocalDate date = LocalDate.of(2026, 8, 12);
        PageRequest pageable = PageRequest.of(0, 10);
        TaskEntity task = new TaskEntity();
        task.setId(100L);
        task.setTitle("Cobrar fotos");
        task.setDateExpires(date);
        task.setType(TaskTypeEnum.COBRANCA);
        task.setPriority(TaskPriorityEnum.NORMAL);
        task.setStatus(TaskStatusEnum.PENDING);
        task.setSource(TaskSourceEnum.MANUAL);
        when(taskRepository.findByUserIdAndStatusAndDateExpiresBetweenOrderByDateExpiresAscTimeExpiresAsc(
                1L, TaskStatusEnum.PENDING, date, date, pageable))
                .thenReturn(new PageImpl<>(java.util.List.of(task), pageable, 1));

        var result = taskService.listAllTask(owner, TaskStatusEnum.PENDING, date, date, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(TaskResponseDTO::status)
                .containsExactly(TaskStatusEnum.PENDING);
    }

    @Test
    void shouldRejectInvalidDateRange() {
        UserEntity owner = new UserEntity();
        owner.setId(1L);
        LocalDate startDate = LocalDate.of(2026, 8, 13);
        LocalDate endDate = LocalDate.of(2026, 8, 12);

        assertThatThrownBy(() -> taskService.listAllTask(
                        owner,
                        TaskStatusEnum.PENDING,
                        startDate,
                        endDate,
                        PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A data inicial não pode ser posterior à data final");

        verifyNoInteractions(taskRepository);
    }

    @Test
    void shouldCreateWhatsAppTasksWithUniqueReferencesForTheSameMessage() {
        UserEntity owner = new UserEntity();
        owner.setId(1L);
        AtomicLong sequence = new AtomicLong(100L);
        when(taskRepository.findBySourceReferenceOrSourceReferenceStartingWithOrderByIdAsc(
                "MSG-1", "MSG-1:"))
                .thenReturn(List.of());
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> {
            TaskEntity task = invocation.getArgument(0);
            task.setId(sequence.getAndIncrement());
            return task;
        });

        List<TaskResponseDTO> results = taskService.createWhatsAppTasks(
                List.of(request(), request()), owner, "MSG-1");

        ArgumentCaptor<TaskEntity> taskCaptor = ArgumentCaptor.forClass(TaskEntity.class);
        verify(taskRepository, org.mockito.Mockito.times(2)).save(taskCaptor.capture());
        assertThat(taskCaptor.getAllValues())
                .extracting(TaskEntity::getSourceReference)
                .containsExactly("MSG-1", "MSG-1:2");
        assertThat(results).extracting(TaskResponseDTO::id).containsExactly(100L, 101L);
    }

    @Test
    void shouldSetManualSourceAndPendingStatusOnManualCreation() {
        UserEntity owner = new UserEntity();
        owner.setId(1L);
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> {
            TaskEntity task = invocation.getArgument(0);
            task.setId(100L);
            return task;
        });

        taskService.createTask(request(), owner);

        ArgumentCaptor<TaskEntity> taskCaptor = ArgumentCaptor.forClass(TaskEntity.class);
        verify(taskRepository).save(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getSource()).isEqualTo(TaskSourceEnum.MANUAL);
        assertThat(taskCaptor.getValue().getStatus()).isEqualTo(TaskStatusEnum.PENDING);
        assertThat(taskCaptor.getValue().getSourceReference()).isNull();
    }

    @Test
    void shouldReturnTheWholeExistingBatchOnWhatsAppRedelivery() {
        TaskEntity first = task(100L, "MSG-1");
        TaskEntity second = task(101L, "MSG-1:2");
        when(taskRepository.findBySourceReferenceOrSourceReferenceStartingWithOrderByIdAsc(
                "MSG-1", "MSG-1:"))
                .thenReturn(List.of(first, second));

        List<TaskResponseDTO> result = taskService.createWhatsAppTasks(
                List.of(request(), request()), new UserEntity(), "MSG-1");

        assertThat(result).extracting(TaskResponseDTO::id).containsExactly(100L, 101L);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void shouldUpdateOnlyTaskOwnedByCurrentUser() {
        UserEntity owner = new UserEntity();
        owner.setId(1L);
        TaskEntity task = task(100L, null);
        when(taskRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        TaskResponseDTO result = taskService.updateTask(
                100L,
                new TaskStatusUpdateRequestDTO(TaskStatusEnum.DONE),
                owner);

        assertThat(result.status()).isEqualTo(TaskStatusEnum.DONE);
        verify(taskRepository).findByIdAndUserId(100L, 1L);
    }

    @Test
    void shouldHideTaskOwnedByAnotherUser() {
        UserEntity owner = new UserEntity();
        owner.setId(2L);
        when(taskRepository.findByIdAndUserId(100L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTask(100L, owner))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tarefa não encontrada");

        verify(taskRepository, never()).delete(any());
    }

    private TaskRequestDTO request() {
        return new TaskRequestDTO(
                null,
                "Imperial",
                "Cobrar fotos",
                "Cobrar fotos do post",
                LocalDate.of(2026, 8, 12),
                LocalTime.of(10, 0),
                TaskTypeEnum.COBRANCA,
                TaskPriorityEnum.NORMAL
        );
    }

    private TaskEntity task(Long id, String sourceReference) {
        TaskEntity task = new TaskEntity();
        task.setId(id);
        task.setTitle("Cobrar fotos");
        task.setDateExpires(LocalDate.of(2026, 8, 12));
        task.setType(TaskTypeEnum.COBRANCA);
        task.setPriority(TaskPriorityEnum.NORMAL);
        task.setStatus(TaskStatusEnum.PENDING);
        task.setSource(TaskSourceEnum.WHATSAPP);
        task.setSourceReference(sourceReference);
        return task;
    }
}
