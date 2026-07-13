package com.north.producoes.service;

import com.north.producoes.controller.dto.response.ContentIdeaResponseDTO;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.ContentIdeaEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ContentIdeaPriorityEnum;
import com.north.producoes.entity.enums.ContentIdeaStatusEnum;
import com.north.producoes.entity.enums.ContentIdeiaFormatEnum;
import com.north.producoes.entity.enums.PostFormatEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.integration.apify.dto.ApifyContentIdeaSignalDTO;
import com.north.producoes.integration.apify.dto.ContentIdeaAiResponseDTO;
import com.north.producoes.repository.ContentIdeaRepository;
import com.north.producoes.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Regra de negócio das Ideias de Conteúdo: transforma o resultado da IA em
 * entidades persistidas (com deduplicação) e expõe listagem e mudança de status.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentIdeaService {

    public static final int MAX_IDEAS_PER_RUN = 5;
    private static final int DEDUP_WINDOW_DAYS = 30;
    private static final int MAX_SIGNALS_IN_PROMPT = 10;

    private final ContentIdeaRepository contentIdeaRepository;
    private final ContentIdeaAiService contentIdeaAiService;
    private final PostRepository postRepository;

    /**
     * Gera até {@value MAX_IDEAS_PER_RUN} ideias a partir dos sinais coletados
     * e persiste as que não são duplicadas recentes. Status inicial: SUGGESTED.
     *
     * @return ideias efetivamente persistidas
     */
    @Transactional
    public List<ContentIdeaEntity> generateAndPersistIdeas(ClientEntity client,
                                                           List<ApifyContentIdeaSignalDTO> signals) {
        if (signals == null || signals.isEmpty()) {
            // Sem sinais reais a IA inventaria tendências — melhor não gerar nada
            log.warn("[ContentIdea] Nenhum sinal coletado para o cliente {} — geração abortada",
                    client.getId());
            return List.of();
        }

        // Rankeia e limita ANTES de chamar a IA: o prompt numera os sinais
        // nesta ordem e o signalIndex retornado é resolvido contra esta lista
        List<ApifyContentIdeaSignalDTO> rankedSignals = signals.stream()
                .sorted((a, b) -> Double.compare(
                        b.score() != null ? b.score() : 0,
                        a.score() != null ? a.score() : 0))
                .limit(MAX_SIGNALS_IN_PROMPT)
                .toList();

        ContentIdeaAiResponseDTO aiResponse =
                contentIdeaAiService.generateIdeasFromSignals(client, rankedSignals, MAX_IDEAS_PER_RUN);

        if (aiResponse.ideas() == null || aiResponse.ideas().isEmpty()) {
            log.warn("[ContentIdea] IA não retornou ideias para o cliente {}", client.getId());
            return List.of();
        }

        Double topScore = rankedSignals.stream()
                .map(ApifyContentIdeaSignalDTO::score)
                .filter(s -> s != null)
                .max(Double::compare)
                .orElse(null);

        String sourceTerms = client.getAiTerms() != null && client.getAiTerms().hashtags() != null
                ? String.join(", ", client.getAiTerms().hashtags())
                : null;

        LocalDateTime dedupCutoff = LocalDateTime.now().minusDays(DEDUP_WINDOW_DAYS);
        List<ContentIdeaEntity> persisted = new ArrayList<>();
        // Dedup dentro do próprio lote — a IA pode repetir título na mesma resposta
        java.util.Set<String> batchTitles = new java.util.HashSet<>();

        for (ContentIdeaAiResponseDTO.GeneratedIdea idea : aiResponse.ideas()) {
            if (persisted.size() >= MAX_IDEAS_PER_RUN) break;
            if (!StringUtils.hasText(idea.title())) continue;

            boolean duplicate = !batchTitles.add(idea.title().trim().toLowerCase())
                    || contentIdeaRepository.existsByClientIdAndTitleIgnoreCaseAndCreatedAtAfter(
                            client.getId(), idea.title().trim(), dedupCutoff);
            if (duplicate) {
                log.info("[ContentIdea] Ideia duplicada ignorada | cliente {} | '{}'",
                        client.getId(), idea.title());
                continue;
            }

            ContentIdeaEntity entity = new ContentIdeaEntity();
            entity.setClient(client);
            entity.setTitle(truncate(idea.title(), 255));
            entity.setHook(truncate(idea.hook(), 255));
            entity.setTheme(truncate(idea.theme(), 255));
            entity.setObjective(truncate(idea.objective(), 255));
            entity.setFormat(parseFormat(idea.format()));
            entity.setReason(truncate(idea.reason(), 1024));
            entity.setPriority(parsePriority(idea.priority()));
            entity.setSignalSummary(truncate(idea.signalSummary(), 1024));
            entity.setSourceTerms(truncate(sourceTerms, 1024));
            entity.setEngagementScore(resolveSignalScore(rankedSignals, idea.signalIndex(), topScore));
            entity.setStatus(ContentIdeaStatusEnum.SUGGESTED);

            persisted.add(contentIdeaRepository.save(entity));
        }

        log.info("[ContentIdea] {} ideia(s) persistida(s) para o cliente {}",
                persisted.size(), client.getId());
        return persisted;
    }

    public List<ContentIdeaResponseDTO> findAll() {
        return contentIdeaRepository.findAllWithClient().stream()
                .map(ContentIdeaResponseDTO::from)
                .toList();
    }

    @Transactional
    public ContentIdeaResponseDTO updateStatus(Long id, ContentIdeaStatusEnum status,
                                               UserEntity currentUser) {
        ContentIdeaEntity idea = contentIdeaRepository.findByIdWithClient(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhuma ideia de conteúdo encontrada com id: " + id));

        // Converter = criar a demanda real no board (uma única vez por ideia)
        if (status == ContentIdeaStatusEnum.CONVERTED && idea.getConvertedPost() == null) {
            idea.setConvertedPost(createDemandFromIdea(idea, currentUser));
        }

        idea.setStatus(status);
        return ContentIdeaResponseDTO.from(contentIdeaRepository.save(idea));
    }

    private PostEntity createDemandFromIdea(ContentIdeaEntity idea, UserEntity currentUser) {
        java.util.Objects.requireNonNull(currentUser,
                "Usuário autenticado é obrigatório para converter ideia em demanda");
        if (idea.getClient() == null) {
            throw new IllegalArgumentException(
                    "Ideia sem cliente vinculado não pode virar demanda.");
        }

        PostEntity post = new PostEntity();
        post.setTitle(idea.getTitle());
        post.setTheme(idea.getTheme() != null ? idea.getTheme() : idea.getTitle());
        post.setObjective(idea.getObjective() != null ? idea.getObjective() : "Definir objetivo");
        post.setStatus(PostStatusEnum.DEMAND);
        // Data provisória — o time ajusta ao aprovar/agendar
        post.setScheduledAt(LocalDateTime.now().plusDays(7));
        post.setClient(idea.getClient());
        post.setUser(currentUser);
        post.setFormat(idea.getFormat() == ContentIdeiaFormatEnum.CAROUSEL
                ? PostFormatEnum.CAROUSEL
                : PostFormatEnum.IMAGE);
        return postRepository.save(post);
    }

    /**
     * Resolve o score do sinal apontado pela IA (signalIndex é 1-based na
     * ordem do prompt). Índice ausente ou inválido cai no melhor score do lote.
     */
    private static Double resolveSignalScore(List<ApifyContentIdeaSignalDTO> rankedSignals,
                                             Integer signalIndex, Double fallback) {
        if (signalIndex == null || signalIndex < 1 || signalIndex > rankedSignals.size()) {
            return fallback;
        }
        Double score = rankedSignals.get(signalIndex - 1).score();
        return score != null ? score : fallback;
    }

    private static ContentIdeiaFormatEnum parseFormat(String format) {
        if (!StringUtils.hasText(format)) return ContentIdeiaFormatEnum.FEED;
        try {
            return ContentIdeiaFormatEnum.valueOf(format.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ContentIdeiaFormatEnum.FEED;
        }
    }

    private static ContentIdeaPriorityEnum parsePriority(String priority) {
        if (!StringUtils.hasText(priority)) return ContentIdeaPriorityEnum.MEDIUM;
        try {
            return ContentIdeaPriorityEnum.valueOf(priority.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ContentIdeaPriorityEnum.MEDIUM;
        }
    }

    private static String truncate(String text, int max) {
        if (text == null) return null;
        String trimmed = text.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
