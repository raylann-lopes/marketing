package com.north.producoes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.north.producoes.controller.dto.request.TaskRequestDTO;
import com.north.producoes.controller.dto.response.TaskResponseDTO;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.WhatsAppTaskEventEntity;
import com.north.producoes.entity.enums.TaskPriorityEnum;
import com.north.producoes.entity.enums.TaskSourceEnum;
import com.north.producoes.entity.enums.TaskStatusEnum;
import com.north.producoes.entity.enums.TaskTypeEnum;
import com.north.producoes.entity.enums.UserRoleEnum;
import com.north.producoes.integration.evolutionApi.EvolutionApiClient;
import com.north.producoes.integration.evolutionApi.dto.EvolutionWebhookEventDTO;
import com.north.producoes.integration.openai.dto.TaskAiInterpretationDTO;
import com.north.producoes.repository.ClientRepository;
import com.north.producoes.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppTaskServiceTest {

    @Mock
    private TaskAiInterpreterService taskAiInterpreterService;
    @Mock
    private TaskService taskService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private EvolutionApiClient evolutionApiClient;
    @Mock
    private TaskAudioTranscriptionService taskAudioTranscriptionService;
    @Mock
    private WhatsAppTaskInboxService inboxService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WhatsAppTaskService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "tasksInstance", "north-tasks");
        ReflectionTestUtils.setField(service, "tasksApiKey", "tasks-api-key");
        ReflectionTestUtils.setField(service, "defaultUserId", 1L);
        ReflectionTestUtils.setField(service, "zoneId", "America/Sao_Paulo");
        ReflectionTestUtils.setField(service, "allowedNumbers", "5511999999999");
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
        ReflectionTestUtils.setField(service, "retryDelayMs", 60000L);
        ReflectionTestUtils.setField(service, "processingTimeoutMs", 600000L);
    }

    @Test
    void shouldCreateTaskAndReplyToSender() {
        UserEntity admin = admin();
        ClientEntity client = client();
        TaskAiInterpretationDTO interpretation = new TaskAiInterpretationDTO(
                "Cobrar fotos", "Cobrar fotos do post", "Imperial", "2026-08-12", "10:00",
                TaskTypeEnum.COBRANCA, TaskPriorityEnum.NORMAL
        );
        TaskResponseDTO created = response(client);

        when(taskService.findAllBySourceMessageId("MSG-1")).thenReturn(List.of());
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(clientRepository.findByStatus(any())).thenReturn(List.of(client));
        when(taskAiInterpreterService.interpret(any(), any(), any())).thenReturn(List.of(interpretation));
        when(taskService.createWhatsAppTasks(any(), eq(admin), eq("MSG-1"))).thenReturn(List.of(created));

        service.processEvent(event(false, "north-tasks"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TaskRequestDTO>> requestCaptor = ArgumentCaptor.forClass(List.class);
        verify(taskService).createWhatsAppTasks(requestCaptor.capture(), eq(admin), eq("MSG-1"));
        assertThat(requestCaptor.getValue()).singleElement().satisfies(request -> {
            assertThat(request.clientId()).isEqualTo(10L);
            assertThat(request.title()).isEqualTo("Cobrar fotos");
        });
        verify(evolutionApiClient).sendText(eq("5511999999999@s.whatsapp.net"), eq("north-tasks"),
                eq("tasks-api-key"),
                org.mockito.ArgumentMatchers.contains("Tarefa criada com sucesso"));
    }

    @Test
    void shouldIgnoreMessagesFromTheSystem() {
        service.processEvent(event(true, "north-tasks"));

        verifyNoInteractions(taskAiInterpreterService, taskService, userRepository, clientRepository,
                evolutionApiClient, taskAudioTranscriptionService);
    }

    @Test
    void shouldIgnoreAnotherEvolutionInstance() {
        service.processEvent(event(false, "north-approvals"));

        verifyNoInteractions(taskAiInterpreterService, taskService, userRepository, clientRepository,
                evolutionApiClient, taskAudioTranscriptionService);
    }

    @Test
    void shouldIgnoreUnauthorizedSender() {
        ReflectionTestUtils.setField(service, "allowedNumbers", "5511888888888");

        service.processEvent(event(false, "north-tasks"));

        verifyNoInteractions(taskAiInterpreterService, taskService, userRepository, clientRepository,
                evolutionApiClient, taskAudioTranscriptionService);
    }

    @Test
    void shouldNotCreateDuplicateTask() {
        when(taskService.findAllBySourceMessageId("MSG-1")).thenReturn(List.of(response(client())));

        service.processEvent(event(false, "north-tasks"));

        verify(taskService, never()).createWhatsAppTasks(any(), any(), any());
        verifyNoInteractions(taskAiInterpreterService, userRepository, clientRepository);
        verify(evolutionApiClient).sendText(eq("5511999999999@s.whatsapp.net"), eq("north-tasks"),
                eq("tasks-api-key"),
                org.mockito.ArgumentMatchers.contains("Tarefa criada com sucesso"));
    }

    @Test
    void shouldTranscribeAudioBeforeCreatingTask() {
        UserEntity admin = admin();
        ClientEntity client = client();
        TaskAiInterpretationDTO interpretation = new TaskAiInterpretationDTO(
                "Cobrar fotos", "Cobrar fotos do post", "Imperial", "2026-08-12", "10:00",
                TaskTypeEnum.COBRANCA, TaskPriorityEnum.NORMAL
        );

        when(taskAudioTranscriptionService.transcribe(any(), eq("north-tasks"), eq("tasks-api-key")))
                .thenReturn("Lembrar de cobrar as fotos da Imperial às dez horas");
        when(taskService.findAllBySourceMessageId("AUDIO-1")).thenReturn(List.of());
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(clientRepository.findByStatus(any())).thenReturn(List.of(client));
        when(taskAiInterpreterService.interpret(any(), any(), any())).thenReturn(List.of(interpretation));
        when(taskService.createWhatsAppTasks(any(), eq(admin), eq("AUDIO-1")))
                .thenReturn(List.of(response(client)));

        service.processEvent(audioEvent());

        verify(taskAiInterpreterService).interpret(
                eq("Lembrar de cobrar as fotos da Imperial às dez horas"), any(), any());
        verify(taskService).createWhatsAppTasks(any(), eq(admin), eq("AUDIO-1"));
    }

    @Test
    void shouldCreateMultipleTasksAndSendOneBatchConfirmation() {
        UserEntity admin = admin();
        ClientEntity client = client();
        TaskAiInterpretationDTO first = new TaskAiInterpretationDTO(
                "Cobrar fotos", null, "Imperial", "2026-08-13", "09:00",
                TaskTypeEnum.COBRANCA, TaskPriorityEnum.URGENT
        );
        TaskAiInterpretationDTO second = new TaskAiInterpretationDTO(
                "Revisar calendário editorial", null, null, "2026-08-13", "11:30",
                TaskTypeEnum.TAREFA, TaskPriorityEnum.HIGH
        );
        TaskResponseDTO firstCreated = response(client);
        TaskResponseDTO secondCreated = new TaskResponseDTO(
                101L, null, null, "Revisar calendário editorial", null,
                LocalDate.of(2026, 8, 13), LocalTime.of(11, 30),
                TaskTypeEnum.TAREFA, TaskPriorityEnum.HIGH, TaskStatusEnum.PENDING,
                TaskSourceEnum.WHATSAPP, LocalDateTime.now(), null
        );

        when(taskService.findAllBySourceMessageId("MSG-1")).thenReturn(List.of());
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(clientRepository.findByStatus(any())).thenReturn(List.of(client));
        when(taskAiInterpreterService.interpret(any(), any(), any())).thenReturn(List.of(first, second));
        when(taskService.createWhatsAppTasks(any(), eq(admin), eq("MSG-1")))
                .thenReturn(List.of(firstCreated, secondCreated));

        service.processEvent(event(false, "north-tasks"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TaskRequestDTO>> requestCaptor = ArgumentCaptor.forClass(List.class);
        verify(taskService).createWhatsAppTasks(requestCaptor.capture(), eq(admin), eq("MSG-1"));
        assertThat(requestCaptor.getValue()).hasSize(2);
        verify(evolutionApiClient).sendText(
                eq("5511999999999@s.whatsapp.net"),
                eq("north-tasks"),
                eq("tasks-api-key"),
                org.mockito.ArgumentMatchers.contains("2 tarefas criadas com sucesso"));
    }

    @Test
    void shouldPreserveUnregisteredClientAndRemoveItFromTitle() {
        UserEntity admin = admin();
        ClientEntity registeredClient = client();
        TaskAiInterpretationDTO interpretation = new TaskAiInterpretationDTO(
                "Fazer follow-up com a Tower", null, "Tower", "2026-08-20", "16:00",
                TaskTypeEnum.FOLLOW_UP, TaskPriorityEnum.NORMAL
        );
        TaskResponseDTO created = new TaskResponseDTO(
                102L, null, "Tower", "Fazer follow-up", null,
                LocalDate.of(2026, 8, 20), LocalTime.of(16, 0),
                TaskTypeEnum.FOLLOW_UP, TaskPriorityEnum.NORMAL, TaskStatusEnum.PENDING,
                TaskSourceEnum.WHATSAPP, LocalDateTime.now(), null
        );

        when(taskService.findAllBySourceMessageId("MSG-1")).thenReturn(List.of());
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(clientRepository.findByStatus(any())).thenReturn(List.of(registeredClient));
        when(taskAiInterpreterService.interpret(any(), any(), any())).thenReturn(List.of(interpretation));
        when(taskService.createWhatsAppTasks(any(), eq(admin), eq("MSG-1")))
                .thenReturn(List.of(created));

        service.processEvent(event(false, "north-tasks", "Dia 20 às 16h, fazer follow-up com a Tower."));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TaskRequestDTO>> requestCaptor = ArgumentCaptor.forClass(List.class);
        verify(taskService).createWhatsAppTasks(requestCaptor.capture(), eq(admin), eq("MSG-1"));
        assertThat(requestCaptor.getValue()).singleElement().satisfies(request -> {
            assertThat(request.clientId()).isNull();
            assertThat(request.clientName()).isEqualTo("Tower");
            assertThat(request.title()).isEqualTo("Fazer follow-up");
        });
    }

    @Test
    void shouldPersistFailureForRetryWhenProcessingStoredEvent() throws Exception {
        WhatsAppTaskEventEntity storedEvent = new WhatsAppTaskEventEntity();
        storedEvent.setId(50L);
        storedEvent.setPayload("{payload}");
        WhatsAppTaskEventEntity failedEvent = new WhatsAppTaskEventEntity();
        failedEvent.setId(50L);
        failedEvent.setAttempts(1);

        when(inboxService.reserve(eq(50L), eq(3), any(Duration.class))).thenReturn(true);
        when(inboxService.findById(50L)).thenReturn(storedEvent);
        when(objectMapper.readValue("{payload}", EvolutionWebhookEventDTO.class))
                .thenReturn(event(false, "north-tasks"));
        when(taskService.findAllBySourceMessageId("MSG-1")).thenReturn(List.of());
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin()));
        when(clientRepository.findByStatus(any())).thenReturn(List.of(client()));
        when(taskAiInterpreterService.interpret(any(), any(), any()))
                .thenThrow(new RuntimeException("OpenAI indisponível"));
        when(inboxService.markFailed(eq(50L), eq("OpenAI indisponível"), any(Duration.class)))
                .thenReturn(failedEvent);

        service.processStoredEvent(50L);

        verify(inboxService).markFailed(eq(50L), eq("OpenAI indisponível"), any(Duration.class));
        verify(inboxService, never()).markCompleted(50L);
    }

    private EvolutionWebhookEventDTO event(boolean fromMe, String instance) {
        return event(fromMe, instance, "Criar tarefa para a Imperial");
    }

    private EvolutionWebhookEventDTO event(boolean fromMe, String instance, String text) {
        return new EvolutionWebhookEventDTO(
                "messages.upsert",
                instance,
                new EvolutionWebhookEventDTO.Data(
                        new EvolutionWebhookEventDTO.Key("5511999999999@s.whatsapp.net", fromMe, "MSG-1"),
                        new EvolutionWebhookEventDTO.Message(text, null, null),
                        "conversation"
                )
        );
    }

    private EvolutionWebhookEventDTO audioEvent() {
        return new EvolutionWebhookEventDTO(
                "messages.upsert",
                "north-tasks",
                new EvolutionWebhookEventDTO.Data(
                        new EvolutionWebhookEventDTO.Key("5511999999999@s.whatsapp.net", false, "AUDIO-1"),
                        new EvolutionWebhookEventDTO.Message(null, null, null),
                        "audioMessage"
                )
        );
    }

    private UserEntity admin() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setRole(UserRoleEnum.ADMIN);
        user.setActive(true);
        return user;
    }

    private ClientEntity client() {
        ClientEntity client = new ClientEntity();
        client.setId(10L);
        client.setName("Imperial");
        return client;
    }

    private TaskResponseDTO response(ClientEntity client) {
        return new TaskResponseDTO(
                100L,
                client.getId(),
                client.getName(),
                "Cobrar fotos",
                "Cobrar fotos do post",
                LocalDate.of(2026, 8, 12),
                LocalTime.of(10, 0),
                TaskTypeEnum.COBRANCA,
                TaskPriorityEnum.NORMAL,
                TaskStatusEnum.PENDING,
                TaskSourceEnum.WHATSAPP,
                LocalDateTime.now(),
                null
        );
    }
}
