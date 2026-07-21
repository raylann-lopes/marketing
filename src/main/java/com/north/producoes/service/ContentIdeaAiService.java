package com.north.producoes.service;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.exception.InvalidClientDataException;
import com.north.producoes.integration.apify.dto.ApifyContentIdeaSignalDTO;
import com.north.producoes.integration.apify.dto.ContentIdeaAiResponseDTO;
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
            + "O campo 'hashtags' é usado para buscar posts diretamente na página de cada hashtag do "
            + "Instagram — cada item deve ser uma hashtag real e provável de existir no Instagram "
            + "brasileiro para esse nicho: uma palavra só, sem espaço, sem acento, minúscula. "
            + "Hashtags hiperespecíficas do nicho (ex: nome composto do serviço) costumam ter poucos ou "
            + "nenhum post — a página delas no Instagram só mostra um punhado de resultados sem login. "
            + "Para ter volume de posts de verdade, gere de 4 a 6 hashtags MISTURANDO: (1) 2-3 hashtags "
            + "amplas e populares do setor/área de atuação (ex: para um técnico em informática, algo como "
            + "'tecnologia', 'informatica', 'suportetecnico', 'ti'), mesmo que menos específicas, e (2) "
            + "2-3 hashtags mais específicas do nicho exato do cliente. Priorize sempre as que você tem "
            + "mais confiança de que são realmente usadas em português no Brasil. "
            + "O campo 'searchTerms' é usado como busca de nome de perfil (não de tópico/hashtag) "
            + "e complementa a busca por hashtag quando ela não trouxer volume suficiente. "
            + "Responda APENAS com JSON válido, sem texto antes ou depois, sem markdown, sem blocos de código. "
            + "O JSON deve conter exatamente os campos 'searchTerms' e 'hashtags', ambos contendo listas de strings.";

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.ai.openai.chat.options.model:gpt-4o}")
    private String model;

    public ContentIdeaAiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
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

    private static final String IDEAS_SYSTEM_INSTRUCTION = """
            Você é um estrategista de conteúdo que transforma tendências virais do Instagram \
            em ideias de posts acionáveis para clientes de uma agência.
            Regras obrigatórias:
            - Gere no MÁXIMO %d ideias.
            - NUNCA copie a legenda viral literalmente; adapte a tendência ao nicho e tom de voz do cliente.
            - Cada ideia deve ser aplicável como demanda real de produção.
            - Responda APENAS com JSON válido, sem texto antes ou depois, sem markdown, sem blocos de código.
            O JSON deve ter exatamente este formato:
            {"ideas":[{"title":"...","hook":"...","theme":"...","objective":"...",\
            "format":"REELS|CAROUSEL|STORIES|FEED","reason":"...",\
            "priority":"HIGH|MEDIUM|LOW","signalSummary":"...","signalIndex":1}]}
            Onde: title = título da demanda; hook = gancho viral curto para o post; \
            theme = temática; objective = objetivo de negócio; reason = justificativa curta \
            (máx 300 caracteres) de por que a tendência funciona para o cliente; \
            signalSummary = resumo curto da tendência/sinal viral usado como base; \
            signalIndex = número do sinal listado no prompt que inspirou a ideia.""";

    /**
     * Transforma sinais virais coletados na Apify em ideias de conteúdo
     * adaptadas ao nicho e tom de voz do cliente. Retorna lista vazia em caso
     * de falha da IA — o chamador decide como registrar o erro.
     */
    public ContentIdeaAiResponseDTO generateIdeasFromSignals(ClientEntity client,
                                                             List<ApifyContentIdeaSignalDTO> signals,
                                                             int maxIdeas) {
        try {
            Prompt prompt = new Prompt(
                    List.of(new SystemMessage(String.format(IDEAS_SYSTEM_INSTRUCTION, maxIdeas)),
                            UserMessage.builder().text(buildIdeasPrompt(client, signals)).build()),
                    OpenAiChatOptions.builder()
                            .model(model)
                            .temperature(0.5)
                            .build()
            );
            String json = stripCodeFences(chatClient.prompt(prompt).call().content());
            return objectMapper.readValue(json, ContentIdeaAiResponseDTO.class);
        } catch (Exception e) {
            log.error("Erro ao gerar ideias a partir de sinais para o cliente {}: {}",
                    client.getId(), e.getMessage());
            return new ContentIdeaAiResponseDTO(List.of());
        }
    }

    private String buildIdeasPrompt(ClientEntity client, List<ApifyContentIdeaSignalDTO> signals) {
        StringBuilder sb = new StringBuilder();
        sb.append("Cliente: ").append(client.getName()).append("\n");
        sb.append("Nicho: ").append(client.getNiche()).append("\n");
        sb.append("Tom de voz: ").append(client.getVoiceTone()).append("\n\n");
        sb.append("Sinais virais coletados (numerados — use o número em signalIndex):\n");

        // A numeração segue a ordem da lista recebida: o chamador resolve
        // signalIndex de volta para o sinal usando esta mesma ordem
        for (int i = 0; i < signals.size(); i++) {
            ApifyContentIdeaSignalDTO s = signals.get(i);
            sb.append(i + 1).append(". [score ").append(s.score() != null ? s.score().intValue() : 0)
                    .append(" | ").append(s.type() != null ? s.type() : "post")
                    .append(" | likes ").append(s.likes() != null ? s.likes() : 0)
                    .append(" | comments ").append(s.comments() != null ? s.comments() : 0)
                    .append("] ").append(truncate(s.caption(), 220))
                    .append(s.hashtags() != null && !s.hashtags().isEmpty()
                            ? " (hashtags: " + String.join(", ", s.hashtags().stream().limit(5).toList()) + ")"
                            : "")
                    .append("\n");
        }

        sb.append("\nGere as ideias adaptadas a este cliente.");
        return sb.toString();
    }

    private static String stripCodeFences(String content) {
        if (content == null) return "";
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        return trimmed;
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        String clean = text.replaceAll("\\s+", " ").trim();
        return clean.length() <= max ? clean : clean.substring(0, max) + "…";
    }
}



