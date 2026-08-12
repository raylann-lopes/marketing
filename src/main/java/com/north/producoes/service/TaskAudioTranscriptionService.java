package com.north.producoes.service;

import com.north.producoes.exception.AiIntegrationException;
import com.north.producoes.integration.evolutionApi.EvolutionApiClient;
import com.north.producoes.integration.evolutionApi.dto.EvolutionMediaDownloadResponseDTO;
import com.north.producoes.integration.evolutionApi.dto.EvolutionWebhookEventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Base64;

@Service
@RequiredArgsConstructor
public class TaskAudioTranscriptionService {

    private final EvolutionApiClient evolutionApiClient;
    private final OpenAiAudioTranscriptionModel transcriptionModel;

    @Value("${task.whatsapp.max-audio-bytes:20000000}")
    private int maxAudioBytes;

    public String transcribe(
            EvolutionWebhookEventDTO.Key messageKey,
            String instance,
            String apiKey) {
        EvolutionMediaDownloadResponseDTO media = evolutionApiClient.downloadMedia(messageKey, instance, apiKey);
        if (media == null || !StringUtils.hasText(media.base64())) {
            throw new AiIntegrationException("A Evolution não retornou o conteúdo do áudio.");
        }

        byte[] audio = decode(media.base64());
        if (audio.length > maxAudioBytes) {
            throw new AiIntegrationException("O áudio ultrapassa o limite permitido para transcrição.");
        }

        String fileName = StringUtils.hasText(media.fileName()) ? media.fileName() : messageKey.id() + ".ogg";
        ByteArrayResource audioResource = new ByteArrayResource(audio) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        try {
            String transcription = transcriptionModel.call(audioResource);
            if (!StringUtils.hasText(transcription)) {
                throw new AiIntegrationException("A transcrição do áudio retornou vazia.");
            }
            return transcription.strip();
        } catch (AiIntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiIntegrationException("Não foi possível transcrever o áudio recebido.", ex);
        }
    }

    private byte[] decode(String base64) {
        try {
            String content = base64.contains(",") ? base64.substring(base64.indexOf(',') + 1) : base64;
            return Base64.getDecoder().decode(content);
        } catch (IllegalArgumentException ex) {
            throw new AiIntegrationException("A Evolution retornou um áudio inválido.", ex);
        }
    }
}
