package com.north.producoes.service;

import com.north.producoes.controller.dto.request.TaskRequestDTO;
import com.north.producoes.controller.dto.response.TaskResponseDTO;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ClientStatusEnum;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppTaskService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private final Set<String> messagesInProgress = ConcurrentHashMap.newKeySet();

    private final TaskAiInterpreterService taskAiInterpreterService;
    private final TaskService taskService;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final EvolutionApiClient evolutionApiClient;
    private final TaskAudioTranscriptionService taskAudioTranscriptionService;

    @Value("${evolution.api.tasks-instance:}")
    private String tasksInstance;

    @Value("${evolution.api.tasks-key:}")
    private String tasksApiKey;

    @Value("${task.whatsapp.default-user-id:0}")
    private Long defaultUserId;

    @Value("${task.whatsapp.zone-id:America/Sao_Paulo}")
    private String zoneId;

    @Value("${task.whatsapp.allowed-numbers:}")
    private String allowedNumbers;

    @Async
    public void processEvent(EvolutionWebhookEventDTO event) {
        if (event == null || !"messages.upsert".equals(event.event())) return;
        if (!StringUtils.hasText(tasksInstance) || !tasksInstance.equals(event.instance())) {
            log.warn("[TaskWebhook] Evento ignorado por instância diferente: {}", event.instance());
            return;
        }

        EvolutionWebhookEventDTO.Data data = event.data();
        if (data == null || data.key() == null || Boolean.TRUE.equals(data.key().fromMe())) return;

        String recipient = data.key().remoteJid();
        String messageId = data.key().id();
        if (!StringUtils.hasText(recipient) || !StringUtils.hasText(messageId)) {
            log.debug("[TaskWebhook] Evento sem remetente ou ID. messageType={}", data.messageType());
            return;
        }
        if (!isAllowedSender(recipient)) {
            log.warn("[TaskWebhook] Mensagem ignorada de remetente não autorizado.");
            return;
        }
        if (!messagesInProgress.add(messageId)) {
            log.info("[TaskWebhook] Mensagem {} já está em processamento.", messageId);
            return;
        }

        try {
            String text = extractContent(data);
            if (!StringUtils.hasText(text)) {
                log.info("[TaskWebhook] Tipo de mensagem não suportado: {}", data.messageType());
                return;
            }

            TaskResponseDTO existing = taskService.findBySourceReference(messageId).orElse(null);
            if (existing != null) {
                log.info("[TaskWebhook] Mensagem {} já processada.", messageId);
                sendConfirmationSafely(recipient, existing);
                return;
            }

            UserEntity owner = getTaskOwner();
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(zoneId));
            List<ClientEntity> activeClients = clientRepository.findByStatus(ClientStatusEnum.ACTIVE);
            List<String> clientNames = activeClients.stream()
                    .map(ClientEntity::getName)
                    .toList();

            List<TaskAiInterpretationDTO> interpretations = taskAiInterpreterService.interpret(text, now, clientNames);
            List<TaskRequestDTO> requests = interpretations.stream()
                    .map(interpretation -> toTaskRequest(
                            interpretation,
                            findClient(interpretation.clientName(), text, activeClients)))
                    .toList();
            List<TaskResponseDTO> created = taskService.createWhatsAppTasks(requests, owner, messageId);

            sendConfirmationSafely(recipient, created);
            log.info("[TaskWebhook] {} tarefa(s) criada(s) pela mensagem {}.", created.size(), messageId);
        } catch (Exception ex) {
            log.error("[TaskWebhook] Falha ao criar tarefa da mensagem {}: {}", messageId, ex.getMessage(), ex);
            sendFailure(recipient);
        } finally {
            messagesInProgress.remove(messageId);
        }
    }

    private UserEntity getTaskOwner() {
        if (defaultUserId == null || defaultUserId <= 0) {
            throw new IllegalStateException("WHATSAPP_TASK_DEFAULT_USER_ID não configurado.");
        }

        UserEntity user = userRepository.findById(defaultUserId)
                .orElseThrow(() -> new IllegalStateException("Usuário padrão das tarefas não encontrado."));
        if (!user.isActive() || user.getRole() != UserRoleEnum.ADMIN) {
            throw new IllegalStateException("Usuário padrão das tarefas deve ser um administrador ativo.");
        }
        return user;
    }

    private ClientEntity findClient(
            String clientName,
            String originalMessage,
            List<ClientEntity> activeClients) {
        if (!StringUtils.hasText(clientName)) return null;

        String normalizedClientName = normalizeText(clientName);
        List<ClientEntity> candidates = activeClients.stream()
                .filter(client -> {
                    String registeredName = normalizeText(client.getName());
                    return registeredName.equals(normalizedClientName)
                            || registeredName.contains(normalizedClientName)
                            || normalizedClientName.contains(registeredName);
                })
                .toList();

        if (candidates.size() != 1) return null;
        ClientEntity candidate = candidates.getFirst();
        return isClientMentioned(originalMessage, clientName, candidate, activeClients)
                ? candidate
                : null;
    }

    private TaskRequestDTO toTaskRequest(TaskAiInterpretationDTO interpretation, ClientEntity client) {
        LocalDate date = LocalDate.parse(interpretation.dateExpires());
        LocalTime time = StringUtils.hasText(interpretation.timeExpires())
                ? LocalTime.parse(interpretation.timeExpires())
                : null;

        String clientName = client != null ? client.getName() : trimToLength(interpretation.clientName(), 120);
        String title = removeClientFromTitle(interpretation.title(), clientName);
        return new TaskRequestDTO(
                client != null ? client.getId() : null,
                clientName,
                trimToLength(title, 60),
                trimToLength(interpretation.description(), 500),
                date,
                time,
                interpretation.type() != null ? interpretation.type() : TaskTypeEnum.TAREFA,
                interpretation.priority() != null ? interpretation.priority() : TaskPriorityEnum.NORMAL,
                TaskStatusEnum.PENDING,
                TaskSourceEnum.WHATSAPP
        );
    }

    private void sendConfirmation(String recipient, TaskResponseDTO task) {
        String schedule = task.dateExpires().format(DATE_FORMAT)
                + (task.timeExpires() != null ? " às " + task.timeExpires().format(TIME_FORMAT) : "");
        String client = StringUtils.hasText(task.clientName()) ? task.clientName() : "Sem cliente";
        String message = """
                Tarefa criada com sucesso.

                Título: %s
                Cliente: %s
                Data: %s
                Tipo: %s
                Prioridade: %s
                """.formatted(task.title(), client, schedule, task.type(), task.priority()).strip();
        evolutionApiClient.sendText(recipient, tasksInstance, tasksApiKey, message);
    }

    private void sendConfirmationSafely(String recipient, TaskResponseDTO task) {
        try {
            sendConfirmation(recipient, task);
        } catch (Exception ex) {
            log.error("[TaskWebhook] Tarefa {} foi criada, mas a confirmação falhou: {}", task.id(), ex.getMessage());
        }
    }

    private void sendConfirmation(String recipient, List<TaskResponseDTO> tasks) {
        if (tasks.size() == 1) {
            sendConfirmation(recipient, tasks.getFirst());
            return;
        }

        StringBuilder message = new StringBuilder("%d tarefas criadas com sucesso.\n".formatted(tasks.size()));
        for (int index = 0; index < tasks.size(); index++) {
            TaskResponseDTO task = tasks.get(index);
            String schedule = task.dateExpires().format(DATE_FORMAT)
                    + (task.timeExpires() != null ? " às " + task.timeExpires().format(TIME_FORMAT) : "");
            String client = StringUtils.hasText(task.clientName()) ? task.clientName() : "Sem cliente";
            message.append("\n%d. %s\n%s | %s | %s | %s\n".formatted(
                    index + 1,
                    task.title(),
                    client,
                    schedule,
                    task.type(),
                    task.priority()));
        }
        evolutionApiClient.sendText(recipient, tasksInstance, tasksApiKey, message.toString().strip());
    }

    private void sendConfirmationSafely(String recipient, List<TaskResponseDTO> tasks) {
        try {
            sendConfirmation(recipient, tasks);
        } catch (Exception ex) {
            log.error("[TaskWebhook] {} tarefa(s) foram criadas, mas a confirmação falhou: {}",
                    tasks.size(), ex.getMessage());
        }
    }

    private void sendFailure(String recipient) {
        try {
            evolutionApiClient.sendText(
                    recipient,
                    tasksInstance,
                    tasksApiKey,
                    "Não consegui criar a tarefa. Envie novamente informando o que fazer e, se possível, a data e o horário."
            );
        } catch (Exception ex) {
            log.error("[TaskWebhook] Também falhou ao enviar a mensagem de erro: {}", ex.getMessage());
        }
    }

    private String extractText(EvolutionWebhookEventDTO.Data data) {
        if (data.message() == null) return null;
        if (StringUtils.hasText(data.message().conversation())) return data.message().conversation();
        if (data.message().extendedTextMessage() != null) return data.message().extendedTextMessage().text();
        return null;
    }

    private String extractContent(EvolutionWebhookEventDTO.Data data) {
        if ("audioMessage".equals(data.messageType())) {
            return taskAudioTranscriptionService.transcribe(data.key(), tasksInstance, tasksApiKey);
        }
        return extractText(data);
    }

    private String trimToLength(String value, int maxLength) {
        if (!StringUtils.hasText(value)) return value;
        String trimmed = value.strip();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private boolean isClientMentioned(
            String message,
            String extractedName,
            ClientEntity candidate,
            List<ClientEntity> activeClients) {
        String normalizedMessage = normalizeText(message);
        String normalizedExtractedName = normalizeText(extractedName);
        if (containsWholeTerm(normalizedMessage, normalizedExtractedName)) return true;

        return Arrays.stream(normalizeText(candidate.getName()).split("\\s+"))
                .filter(token -> token.length() >= 4)
                .filter(token -> activeClients.stream()
                        .filter(client -> containsWholeTerm(normalizeText(client.getName()), token))
                        .count() == 1)
                .anyMatch(token -> containsWholeTerm(normalizedMessage, token));
    }

    private String removeClientFromTitle(String title, String clientName) {
        if (!StringUtils.hasText(title) || !StringUtils.hasText(clientName)) return title;

        String prepositions = "(?:com\\s+a|com\\s+o|para\\s+a|para\\s+o|da|do|de|com|para|a|o)";
        String pattern = "(?iu)\\s+" + prepositions + "?\\s*" + Pattern.quote(clientName.strip()) + "(?=\\s*$|[.,:;])";
        String cleaned = title.replaceFirst(pattern, "").replaceAll("\\s{2,}", " ").strip();
        return cleaned.isBlank() ? title : cleaned;
    }

    private boolean containsWholeTerm(String text, String term) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(term)) return false;
        return Pattern.compile("(^|\\s)" + Pattern.quote(term) + "($|\\s)").matcher(text).find();
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9]+", " ")
                .strip()
                .toLowerCase(Locale.ROOT);
    }

    private boolean isAllowedSender(String remoteJid) {
        if (!StringUtils.hasText(allowedNumbers)) {
            log.error("[TaskWebhook] WHATSAPP_TASK_ALLOWED_NUMBERS não configurado.");
            return false;
        }

        String senderNumber = onlyDigits(remoteJid.split("@")[0]);
        return Arrays.stream(allowedNumbers.split(","))
                .map(this::onlyDigits)
                .anyMatch(senderNumber::equals);
    }

    private String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}
