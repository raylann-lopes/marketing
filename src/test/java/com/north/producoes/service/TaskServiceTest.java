package com.north.producoes.service;

import com.north.producoes.controller.dto.request.TaskRequestDTO;
import com.north.producoes.controller.dto.response.TaskResponseDTO;
import com.north.producoes.entity.TaskEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.TaskPriorityEnum;
import com.north.producoes.entity.enums.TaskSourceEnum;
import com.north.producoes.entity.enums.TaskStatusEnum;
import com.north.producoes.entity.enums.TaskTypeEnum;
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
    void shouldPersistEvolutionMessageIdWhenCreatingWhatsAppTask() {
        UserEntity owner = new UserEntity();
        owner.setId(1L);
        when(taskRepository.findBySourceReference("MSG-1")).thenReturn(Optional.empty());
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> {
            TaskEntity task = invocation.getArgument(0);
            task.setId(100L);
            return task;
        });

        TaskResponseDTO result = taskService.createWhatsAppTask(request(), owner, "MSG-1");

        ArgumentCaptor<TaskEntity> taskCaptor = ArgumentCaptor.forClass(TaskEntity.class);
        verify(taskRepository).save(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getSourceReference()).isEqualTo("MSG-1");
        assertThat(taskCaptor.getValue().getSource()).isEqualTo(TaskSourceEnum.WHATSAPP);
        assertThat(result.id()).isEqualTo(100L);
    }

    @Test
    void shouldReturnExistingTaskWithoutSavingDuplicate() {
        TaskEntity existing = new TaskEntity();
        existing.setId(100L);
        existing.setTitle("Cobrar fotos");
        existing.setDateExpires(LocalDate.of(2026, 8, 12));
        existing.setType(TaskTypeEnum.COBRANCA);
        existing.setPriority(TaskPriorityEnum.NORMAL);
        existing.setStatus(TaskStatusEnum.PENDING);
        existing.setSource(TaskSourceEnum.WHATSAPP);
        existing.setSourceReference("MSG-1");
        when(taskRepository.findBySourceReference("MSG-1")).thenReturn(Optional.of(existing));

        TaskResponseDTO result = taskService.createWhatsAppTask(request(), new UserEntity(), "MSG-1");

        assertThat(result.id()).isEqualTo(100L);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void shouldCreateWhatsAppTasksWithUniqueReferencesForTheSameMessage() {
        UserEntity owner = new UserEntity();
        owner.setId(1L);
        AtomicLong sequence = new AtomicLong(100L);
        when(taskRepository.findBySourceReference("MSG-1")).thenReturn(Optional.empty());
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

    private TaskRequestDTO request() {
        return new TaskRequestDTO(
                null,
                "Imperial",
                "Cobrar fotos",
                "Cobrar fotos do post",
                LocalDate.of(2026, 8, 12),
                LocalTime.of(10, 0),
                TaskTypeEnum.COBRANCA,
                TaskPriorityEnum.NORMAL,
                TaskStatusEnum.PENDING,
                TaskSourceEnum.WHATSAPP
        );
    }
}
