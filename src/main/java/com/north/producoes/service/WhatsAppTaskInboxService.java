package com.north.producoes.service;

import com.north.producoes.entity.WhatsAppTaskEventEntity;
import com.north.producoes.entity.enums.WhatsAppTaskEventStatusEnum;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.integration.evolutionApi.dto.EvolutionWebhookEventDTO;
import com.north.producoes.repository.WhatsAppTaskEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WhatsAppTaskInboxService {

    private static final int RETRY_BATCH_SIZE = 50;
    private final WhatsAppTaskEventRepository eventRepository;

    @Transactional
    public Optional<Long> enqueue(String rawPayload, EvolutionWebhookEventDTO event) {
        String messageId = extractMessageId(event);
        if (!StringUtils.hasText(messageId)) return Optional.empty();

        eventRepository.insertIfAbsent(messageId, rawPayload);
        return eventRepository.findByMessageId(messageId).map(WhatsAppTaskEventEntity::getId);
    }

    @Transactional
    public boolean reserve(Long eventId, int maxAttempts, Duration processingTimeout) {
        LocalDateTime now = LocalDateTime.now();
        return eventRepository.reserveForProcessing(
                eventId,
                maxAttempts,
                now,
                now.minus(processingTimeout)) == 1;
    }

    @Transactional(readOnly = true)
    public WhatsAppTaskEventEntity findById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento de tarefa do WhatsApp não encontrado"));
    }

    @Transactional
    public void markCompleted(Long eventId) {
        WhatsAppTaskEventEntity event = findEntity(eventId);
        event.setStatus(WhatsAppTaskEventStatusEnum.COMPLETED);
        event.setNextAttemptAt(null);
        event.setLastError(null);
    }

    @Transactional
    public WhatsAppTaskEventEntity markFailed(Long eventId, String error, Duration retryDelay) {
        WhatsAppTaskEventEntity event = findEntity(eventId);
        event.setStatus(WhatsAppTaskEventStatusEnum.FAILED);
        event.setLastError(trim(error, 500));
        event.setNextAttemptAt(LocalDateTime.now().plus(retryDelay.multipliedBy(Math.max(event.getAttempts(), 1))));
        return event;
    }

    @Transactional(readOnly = true)
    public List<Long> findProcessableIds(int maxAttempts, Duration processingTimeout) {
        LocalDateTime now = LocalDateTime.now();
        return eventRepository.findProcessableIds(
                List.of(WhatsAppTaskEventStatusEnum.PENDING, WhatsAppTaskEventStatusEnum.FAILED),
                WhatsAppTaskEventStatusEnum.PROCESSING,
                maxAttempts,
                now,
                now.minus(processingTimeout),
                PageRequest.of(0, RETRY_BATCH_SIZE));
    }

    private WhatsAppTaskEventEntity findEntity(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento de tarefa do WhatsApp não encontrado"));
    }

    private String extractMessageId(EvolutionWebhookEventDTO event) {
        if (event == null || !"messages.upsert".equals(event.event())) return null;
        if (event.data() == null || event.data().key() == null) return null;
        return event.data().key().id();
    }

    private String trim(String value, int maxLength) {
        if (!StringUtils.hasText(value)) return "Falha sem detalhes";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
