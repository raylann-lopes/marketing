package com.north.producoes.service;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.ContentIdeaEntity;
import com.north.producoes.entity.ContentIdeaRunsEntity;
import com.north.producoes.entity.enums.ContentIdeaRunsStatusEnum;
import com.north.producoes.exception.AiIntegrationException;
import com.north.producoes.exception.ApifyIntegrationException;
import com.north.producoes.exception.ResourceAlreadyExistsException;
import com.north.producoes.integration.apify.ApifyClient;
import com.north.producoes.integration.apify.dto.ApifyContentIdeaSignalDTO;
import com.north.producoes.integration.apify.dto.ApifyInstagramPostResponseDTO;
import com.north.producoes.integration.apify.dto.ApifyInstagramRunRequestDTO;
import com.north.producoes.integration.apify.dto.ContentIdeaTermsDTO;
import com.north.producoes.repository.ClientRepository;
import com.north.producoes.repository.ContentIdeaRunsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApifySearchContentIdeaService {

    // Polling da Apify: 5s de intervalo, máx. 60 tentativas (5 min) — evita
    // request presa para sempre se o Actor travar em RUNNING
    private static final long POLL_INTERVAL_MS = 5000;
    private static final int MAX_POLL_ATTEMPTS = 60;

    private final ContentIdeaAiService contentIdeaAiService;
    private final ContentIdeaService contentIdeaService;
    private final ClientRepository clientRepository;
    private final ApifyClient apifyClient;
    private final ContentIdeaRunsRepository contentIdeaRunsRepository;

    // O actor cobra por resultado (~US$2,30/1000): perfis × posts é o custo
    // de cada coleta. 3×8 = 24 resultados ≈ US$0,06/run.
    private final int profilesPerSearch;
    private final int postsPerProfile;

    public ApifySearchContentIdeaService(
            ContentIdeaAiService contentIdeaAiService,
            ContentIdeaService contentIdeaService,
            ClientRepository clientRepository,
            ApifyClient apifyClient,
            ContentIdeaRunsRepository contentIdeaRunsRepository,
            @Value("${apify.instagram.search-profiles:3}") int profilesPerSearch,
            @Value("${apify.instagram.posts-per-profile:8}") int postsPerProfile
    ) {
        this.contentIdeaAiService = contentIdeaAiService;
        this.contentIdeaService = contentIdeaService;
        this.clientRepository = clientRepository;
        this.apifyClient = apifyClient;
        this.contentIdeaRunsRepository = contentIdeaRunsRepository;
        this.profilesPerSearch = profilesPerSearch;
        this.postsPerProfile = postsPerProfile;
    }

    public ContentIdeaTermsDTO generateIdeas(ClientEntity client) {
        var terms = contentIdeaAiService.generateContentIdeas(client);
        // Em falha da IA num cliente novo, terms vem null — falha explícita
        // aqui evita NPE mais adiante na coleta
        if (!hasUsableTerms(terms)) {
            throw new AiIntegrationException(
                    "IA não retornou termos de busca para o cliente " + client.getId()
                            + ". Tente novamente.");
        }
        client.setAiTerms(terms);
        clientRepository.save(client);
        return terms;
    }

    private static boolean hasUsableTerms(ContentIdeaTermsDTO terms) {
        return terms != null
                && ((terms.searchTerms() != null && !terms.searchTerms().isEmpty())
                || (terms.hashtags() != null && !terms.hashtags().isEmpty()));
    }

    /**
     * Pipeline completo de coleta: garante termos de busca, coleta sinais na
     * Apify e transforma em ideias persistidas. Se nenhuma hashtag retornar
     * posts, a IA regenera os termos e a coleta é tentada mais uma vez.
     */
    public List<ContentIdeaEntity> collectAndGenerateIdeas(ClientEntity client) {
        // Lock: impede coletas concorrentes (e o custo dobrado) para o mesmo
        // cliente; runs STARTED com mais de 10 min são consideradas órfãs
        boolean collectInProgress = contentIdeaRunsRepository
                .existsByClientIdAndStatusAndStartedAtAfter(
                        client.getId(), ContentIdeaRunsStatusEnum.STARTED,
                        LocalDateTime.now().minusMinutes(10));
        if (collectInProgress) {
            throw new ResourceAlreadyExistsException(
                    "Já existe uma coleta em andamento para este cliente. Aguarde a conclusão.");
        }

        if (!hasUsableTerms(client.getAiTerms())) {
            generateIdeas(client);
        }

        ContentIdeaRunsEntity run = startRun(client);
        try {
            List<ApifyContentIdeaSignalDTO> signals = collectSignals(client);

            if (signals.isEmpty()) {
                // Termos atuais não encontraram nada — regenera com a IA e retenta
                generateIdeas(client);
                signals = collectSignals(client);
            }

            if (signals.isEmpty()) {
                throw new ApifyIntegrationException(
                        "A coleta não encontrou posts virais, mesmo após regenerar os termos. "
                                + "Revise o nicho do cliente.");
            }

            List<ContentIdeaEntity> ideas = contentIdeaService.generateAndPersistIdeas(client, signals);
            finishRun(run, ContentIdeaRunsStatusEnum.SUCESS, ideas.size(), null);
            return ideas;
        } catch (Exception e) {
            finishRun(run, ContentIdeaRunsStatusEnum.FAILED, null, e.getMessage());
            throw e;
        }
    }

    /**
     * Coleta sinais sem persistir ideias — mantido para o endpoint de
     * diagnóstico existente.
     */
    public List<ApifyContentIdeaSignalDTO> searchInstagramSignals(ClientEntity client) {
        ContentIdeaRunsEntity run = startRun(client);
        try {
            List<ApifyContentIdeaSignalDTO> signals = collectSignals(client);
            finishRun(run, ContentIdeaRunsStatusEnum.SUCESS, signals.size(), null);
            return signals;
        } catch (Exception e) {
            finishRun(run, ContentIdeaRunsStatusEnum.FAILED, null, e.getMessage());
            throw e;
        }
    }

    private ContentIdeaRunsEntity startRun(ClientEntity client) {
        ContentIdeaRunsEntity run = new ContentIdeaRunsEntity();
        run.setClient(client);
        run.setNiche(client.getNiche());
        run.setStatus(ContentIdeaRunsStatusEnum.STARTED);
        return contentIdeaRunsRepository.save(run);
    }

    private void finishRun(ContentIdeaRunsEntity run, ContentIdeaRunsStatusEnum status,
                           Integer ideasCount, String errorMessage) {
        run.setStatus(status);
        run.setFinishedAt(LocalDateTime.now());
        if (ideasCount != null) run.setIdeasCount(ideasCount);
        if (errorMessage != null) run.setErrorMessage(truncate(errorMessage, 1024));
        contentIdeaRunsRepository.save(run);
    }

    // Máximo de hashtags tentadas por coleta (cada tentativa é uma execução
    // paga na Apify)
    private static final int MAX_SEARCH_ATTEMPTS = 3;

    // Diversidade: evita que um único perfil domine os sinais enviados à IA
    private static final int MAX_SIGNALS_PER_OWNER = 3;
    private static final int MAX_SIGNALS = 15;

    private List<ApifyContentIdeaSignalDTO> collectSignals(ClientEntity client) {
        // Busca por PERFIS do nicho: os searchTerms da IA ("técnico em
        // informática") casam com o formato de busca de usuários. Fallback
        // para hashtags viradas em texto quando não houver searchTerms.
        // Retorna vazio se nada encontrar; o chamador decide o fallback.
        var terms = client.getAiTerms().searchTerms() != null
                && !client.getAiTerms().searchTerms().isEmpty()
                ? client.getAiTerms().searchTerms()
                : client.getAiTerms().hashtags().stream().map(h -> h.replace("#", "")).toList();

        var searchTerms = terms.stream()
                .map(t -> t == null ? "" : t.trim())
                .filter(t -> !t.isEmpty())
                .distinct()
                .limit(MAX_SEARCH_ATTEMPTS)
                .toList();

        if (searchTerms.isEmpty()) {
            throw new ApifyIntegrationException("Cliente sem termos gerados para a coleta.");
        }

        for (String term : searchTerms) {
            List<ApifyContentIdeaSignalDTO> signals = runSearch(term);
            if (!signals.isEmpty()) {
                return signals;
            }
        }
        return List.of();
    }

    private List<ApifyContentIdeaSignalDTO> runSearch(String searchTerm) {
        var request = new ApifyInstagramRunRequestDTO(
                searchTerm, "user", "posts", profilesPerSearch, postsPerProfile);
        var runResponse = apifyClient.runInstagramScraper(request);
        var apifyRunId = runResponse.data().id();

        String apifyStatus = "RUNNING";
        int attempts = 0;
        while (apifyStatus.equals("RUNNING")) {
            if (++attempts > MAX_POLL_ATTEMPTS) {
                throw new ApifyIntegrationException(
                        "Tempo limite excedido aguardando execução do Actor na Apify.");
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ApifyIntegrationException("Polling interrompido.");
            }
            var poll = apifyClient.getRun(apifyRunId);
            apifyStatus = poll.data().status();

            if (apifyStatus.equals("FAILED")) {
                throw new ApifyIntegrationException("Execução do Actor falhou na Apify.");
            }
        }

        var datasetId = runResponse.data().defaultDatasetId();
        // Dataset vazio vem com um item {"error":"no_items"} — o filtro de
        // caption o descarta. Rankeia por engajamento e limita posts por
        // perfil para os sinais não ficarem dominados por uma única conta.
        var posts = apifyClient.getDatasetItems(datasetId)
                .stream()
                .filter(post -> post.caption() != null)
                .filter(post -> post.likesCount() != null || post.commentsCount() != null)
                .sorted((a, b) -> Double.compare(calculateScore(b), calculateScore(a)))
                .toList();

        java.util.Map<String, Integer> postsPerOwner = new java.util.HashMap<>();
        java.util.List<ApifyContentIdeaSignalDTO> signals = new java.util.ArrayList<>();
        for (ApifyInstagramPostResponseDTO post : posts) {
            if (signals.size() >= MAX_SIGNALS) break;
            String owner = post.ownerUsername() != null ? post.ownerUsername() : "";
            int count = postsPerOwner.merge(owner, 1, Integer::sum);
            if (count > MAX_SIGNALS_PER_OWNER) continue;
            signals.add(new ApifyContentIdeaSignalDTO(
                    post.caption(),
                    post.likesCount(),
                    post.commentsCount(),
                    post.timestamp(),
                    post.type(),
                    post.hashtags(),
                    calculateScore(post)
            ));
        }
        return signals;
    }

    private Double calculateScore(ApifyInstagramPostResponseDTO post) {
        int likes = post.likesCount() != null ? post.likesCount() : 0;
        int comments = post.commentsCount() != null ? post.commentsCount() : 0;
        return (double) (likes + (comments * 2));
    }

    private static String truncate(String text, int max) {
        if (text == null) return null;
        return text.length() <= max ? text : text.substring(0, max);
    }
}
