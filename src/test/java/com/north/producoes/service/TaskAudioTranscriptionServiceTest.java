package com.north.producoes.service;

import com.north.producoes.exception.AiIntegrationException;
import com.north.producoes.integration.evolutionApi.EvolutionApiClient;
import com.north.producoes.integration.evolutionApi.dto.EvolutionMediaDownloadResponseDTO;
import com.north.producoes.integration.evolutionApi.dto.EvolutionWebhookEventDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskAudioTranscriptionServiceTest {

    @Mock
    private EvolutionApiClient evolutionApiClient;

    @Mock
    private OpenAiAudioTranscriptionModel transcriptionModel;

    @InjectMocks
    private TaskAudioTranscriptionService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxAudioBytes", 20_000_000);
    }

    @Test
    void shouldDownloadAndTranscribeAudio() {
        EvolutionWebhookEventDTO.Key key = key();
        String base64 = Base64.getEncoder().encodeToString("audio".getBytes(StandardCharsets.UTF_8));
        when(evolutionApiClient.downloadMedia(key, "Task", "task-key"))
                .thenReturn(new EvolutionMediaDownloadResponseDTO("audioMessage", "audio.ogg", "audio/ogg", base64));
        when(transcriptionModel.call(any(Resource.class))).thenReturn("Criar reunião amanhã às 9 horas");

        String result = service.transcribe(key, "Task", "task-key");

        assertThat(result).isEqualTo("Criar reunião amanhã às 9 horas");
    }

    @Test
    void shouldRejectAudioAboveConfiguredLimit() {
        EvolutionWebhookEventDTO.Key key = key();
        ReflectionTestUtils.setField(service, "maxAudioBytes", 2);
        String base64 = Base64.getEncoder().encodeToString("audio".getBytes(StandardCharsets.UTF_8));
        when(evolutionApiClient.downloadMedia(key, "Task", "task-key"))
                .thenReturn(new EvolutionMediaDownloadResponseDTO("audioMessage", "audio.ogg", "audio/ogg", base64));

        assertThatThrownBy(() -> service.transcribe(key, "Task", "task-key"))
                .isInstanceOf(AiIntegrationException.class)
                .hasMessageContaining("limite");
    }

    private EvolutionWebhookEventDTO.Key key() {
        return new EvolutionWebhookEventDTO.Key("5511999999999@s.whatsapp.net", false, "AUDIO-1");
    }
}
