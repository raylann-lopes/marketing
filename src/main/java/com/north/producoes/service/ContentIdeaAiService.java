package com.north.producoes.service;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.exception.InvalidClientDataException;
import com.north.producoes.integration.apify.dto.ContentIdeaTermsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class ContentIdeaAiService {

    private static final Logger log = LoggerFactory.getLogger(ContentIdeaAiService.class);
    private static final String SYSTEM_INSTRUCTION = "Você é um social media procurando os melhores posts para recomendar aos clientes. "
            + "O nicho do cliente é a fonte principal e obrigatória para recomendar os posts. "
            + "Sua resposta deve conter apenas os termos de busca e hashtags, sem análises ou explicações. "
            + "Responda no formato JSON com os campos 'searchTerms' e 'hashtags', ambos contendo listas de strings.";

    private final ChatClient chatClient;

    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o}")
    private String model;

    public ContentIdeaAiService(ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }


    public ContentIdeaTermsDTO generateContentIdeas(ClientEntity client) {
        if (StringUtils.hasText(client.getNiche()))     {
            try {
                Prompt prompt = buildPromptRequest(client);
                String json = chatClient.prompt(prompt).call().content();

                return objectMapper.readValue(json, ContentIdeaTermsDTO.class);
            } catch (Exception e) {
                log.error("Erro ao gerar ideias de conteúdo para o cliente {}: {}", client.getId(), e.getMessage());
            }
        } else {
            throw new InvalidClientDataException("Cliente " + client.getId() + " não possui nicho definido para geração de ideias de conteúdo.");
        }
        return client.getAiTerms();
    }

    private Prompt buildPromptRequest(ClientEntity client){
        String prompt = buildPrompt(client);
        UserMessage userMessage = UserMessage.builder()
                .text(prompt)
                .build();

        return new Prompt(
                List.of(new SystemMessage(SYSTEM_INSTRUCTION),
                        userMessage
                ),
                OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.3)
                        .build()
        );
    }

    public String buildPrompt(ClientEntity client){
        return "Nome do cliente: " + client.getName() + "\n" +
                "Nicho do cliente: " + client.getNiche() + "\n" +
                "Tom de voz do cliente: " + client.getVoiceTone() + "\n" +
                "Recomende os melhores termos de busca e hashtags para esse nicho.";
    }
}



