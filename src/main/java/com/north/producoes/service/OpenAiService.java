package com.north.producoes.service;

import com.north.producoes.entity.PostEntity;
import com.north.producoes.exception.AiIntegrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class OpenAiService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);
    private static final String SYSTEM_INSTRUCTION = "Você é um assistente de redes sociais para conteúdo institucional. "
            + "Imagem e identidade da marca do cliente são as duas fontes principais e obrigatórias. "
            + "A demanda textual do post é complementar. "
            + "Se houver conflito, preserve primeiro a imagem e depois a identidade da marca. "
            + "Sua resposta deve conter apenas o texto final da legenda, sem análises ou explicações.";

    private final ChatClient chatClient;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o}")
    private String model;

    public OpenAiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String generateCaption(PostEntity post, String imageUrl) {
        if (StringUtils.hasText(imageUrl)) {
            try {
                String withImage = doGenerateCaption(post, imageUrl);
                if (!isRefusal(withImage)) {
                    return withImage;
                }
                log.warn("OpenAI retornou recusa para post {} na geracao com imagem. Nova tentativa sem imagem.", post.getId());
            } catch (Exception imageException) {
                log.warn("Falha ao gerar legenda com imagem para o post {}. Nova tentativa sem imagem. Motivo: {}",
                        post.getId(), imageException.getMessage());
            }
        }

        try {
            String textOnly = doGenerateCaption(post, null);
            if (isRefusal(textOnly)) {
                throw new AiIntegrationException("A OpenAI recusou gerar a legenda para esta solicitacao.");
            }
            return textOnly;
        } catch (Exception textOnlyException) {
            throw buildGenerationException(textOnlyException);
        }
    }

    private String doGenerateCaption(PostEntity post, String imageUrl) {
        String content = chatClient.prompt(buildPromptRequest(post, imageUrl)).call().content();
        if (!StringUtils.hasText(content)) {
            throw new AiIntegrationException("A OpenAI retornou uma resposta vazia ao gerar a legenda.");
        }
        return content.trim();
    }

    private Prompt buildPromptRequest(PostEntity post, String imageUrl) {
        String prompt = buildPrompt(post, imageUrl);
        List<Media> media = new ArrayList<>();
        if (StringUtils.hasText(imageUrl)) {
            media.add(new Media(resolveMimeType(imageUrl), URI.create(imageUrl)));
        }

        UserMessage userMessage = UserMessage.builder()
                .text(prompt)
                .media(media)
                .build();

        return new Prompt(
                List.of(
                        new SystemMessage(SYSTEM_INSTRUCTION),
                        userMessage
                ),
                OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.3)
                        .build()
        );
    }

    private String buildPrompt(PostEntity post, String imageUrl) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Você é um redator de mídias sociais experiente. Gere uma legenda atraente.\n\n");
        promptBuilder.append("ORDEM DE PRIORIDADE:\n");
        promptBuilder.append("1. Primeiro analise a imagem e extraia o contexto principal.\n");
        promptBuilder.append("2. Em seguida aplique obrigatoriamente a identidade da marca do cliente (tom, nicho e posicionamento).\n");
        promptBuilder.append("3. Por ultimo use a demanda textual apenas para ajustar objetivo e chamada para ação.\n");
        promptBuilder.append("4. Em caso de conflito, preserve a imagem e depois a identidade da marca.\n\n");

        if (imageUrl != null && !imageUrl.isBlank()) {
            promptBuilder.append("INSTRUÇÕES DE ANÁLISE DE IMAGEM (USO INTERNO):\n");
            promptBuilder.append("1. Identifique marcas, textos, nomes, datas e contexto visual da arte.\n");
            promptBuilder.append("2. Use a mensagem principal da imagem para definir o assunto da legenda.\n");
            promptBuilder.append("3. Se houver contradição entre imagem e demanda, mantenha o foco da imagem.\n\n");
        }

        promptBuilder.append("IDENTIDADE DA MARCA (OBRIGATÓRIO SEGUIR):\n")
                .append("- Cliente: ").append(post.getClient().getName()).append("\n")
                .append("- Nicho: ").append(defaultValue(post.getClient().getNiche(), "Não informado")).append("\n")
                .append("- Tom de Voz: ").append(defaultValue(post.getClient().getVoiceTone(), "Não informado")).append("\n")
                .append("- Diretriz: mantenha consistência com o posicionamento e estilo desse cliente.\n\n");

        promptBuilder.append("DADOS DA DEMANDA (USAR COMO COMPLEMENTO):\n")
                .append("- Título: ").append(post.getTitle()).append("\n")
                .append("- Tema: ").append(post.getTheme()).append("\n")
                .append("- Objetivo: ").append(post.getObjective()).append("\n")
                .append("- Observação: não contradizer imagem e identidade da marca.\n\n");

        promptBuilder.append("REGRAS DE RESPOSTA:\n");
        promptBuilder.append("- Retorne exclusivamente o texto final da legenda para a rede social.\n");
        promptBuilder.append("- Não inclua análises, introduções, explicações ou tópicos sobre a imagem.\n");
        promptBuilder.append("- Use emojis e hashtags estratégicas em português brasileiro.\n");
        promptBuilder.append("- Linguagem deve refletir a identidade da marca do cliente.");
        return promptBuilder.toString();
    }

    private MimeType resolveMimeType(String imageUrl) {
        String normalizedUrl = imageUrl.toLowerCase(Locale.ROOT);
        if (normalizedUrl.endsWith(".png")) {
            return MimeTypeUtils.IMAGE_PNG;
        }
        if (normalizedUrl.endsWith(".gif")) {
            return MimeTypeUtils.IMAGE_GIF;
        }
        if (normalizedUrl.endsWith(".webp")) {
            return MimeTypeUtils.parseMimeType("image/webp");
        }
        return MimeTypeUtils.IMAGE_JPEG;
    }

    private AiIntegrationException buildGenerationException(Exception exception) {
        String message = exception.getMessage();
        if (exception instanceof AiIntegrationException aiIntegrationException) {
            return aiIntegrationException;
        }
        if (message != null && message.contains("api key")) {
            return new AiIntegrationException("A chave da OpenAI nao esta configurada corretamente.", exception);
        }
        if (message != null && (message.contains("dns") || message.contains("resolver"))) {
            return new AiIntegrationException("Falha de resolucao DNS ao acessar a OpenAI. Verifique conectividade e configuracao de rede.", exception);
        }
        return new AiIntegrationException("Falha ao gerar legenda com a OpenAI: " + message, exception);
    }

    private boolean isRefusal(String content) {
        String normalized = content.toLowerCase(Locale.ROOT);
        return normalized.contains("desculpe")
                && (normalized.contains("não posso ajudar") || normalized.contains("nao posso ajudar"))
                || normalized.contains("i can’t help with that")
                || normalized.contains("i can't help with that")
                || normalized.contains("cannot help with that")
                || normalized.contains("i'm sorry, i can't assist with that")
                || normalized.contains("i’m sorry, i can’t assist with that")
                || normalized.contains("can't assist with that")
                || normalized.contains("cannot assist with that");
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
