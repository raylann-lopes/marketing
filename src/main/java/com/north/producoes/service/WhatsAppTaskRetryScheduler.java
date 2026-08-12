package com.north.producoes.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppTaskRetryScheduler {

    private final WhatsAppTaskInboxService inboxService;
    private final WhatsAppTaskService whatsAppTaskService;

    @Value("${task.whatsapp.max-attempts:3}")
    private int maxAttempts;

    @Value("${task.whatsapp.processing-timeout-ms:600000}")
    private long processingTimeoutMs;

    @Scheduled(fixedDelayString = "${task.whatsapp.retry-delay-ms:60000}")
    public void retryPendingEvents() {
        inboxService.findProcessableIds(maxAttempts, Duration.ofMillis(processingTimeoutMs))
                .forEach(whatsAppTaskService::processStoredEvent);
    }
}
