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

import java.text.Normalizer;
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

    // Hashtags batem todas numa ÚNICA execução via directUrls, então usar
    // mais é barato (mais custo marginal por resultado, não por execução).
    // Termos de perfil no fallback já são uma execução paga por termo, então
    // seguem limitados.
    private static final int MAX_HASHTAGS = 6;
    private static final int MAX_PROFILE_FALLBACK_TERMS = 3;

    // A página de hashtag do Instagram limita bastante o que dá pra coletar
    // sem login (geralmente só o grid de "principais" já filtrado por posts
    // recentes) — abaixo desse volume, complementa com busca por perfis em
    // vez de aceitar poucos sinais como resultado final.
    private static final int MIN_SIGNALS_BEFORE_FALLBACK = 6;

    // Diversidade: evita que um único perfil domine os sinais enviados à IA
    private static final int MAX_SIGNALS_PER_OWNER = 3;
    private static final int MAX_SIGNALS = 15;

    private static final String INSTAGRAM_HASHTAG_URL = "https://www.instagram.com/explore/tags/%s/";

    private List<ApifyContentIdeaSignalDTO> collectSignals(ClientEntity client) {
        // Estratégia primária: hashtags reais do nicho via directUrls — a
        // busca por texto (search+searchType) não distingue tópico de nome
        // de perfil, então nunca garantia relevância ao nicho.
        List<ApifyInstagramPostResponseDTO> rawPosts = new java.util.ArrayList<>();
        var hashtags = sanitizedHashtags(client);
        if (!hashtags.isEmpty()) {
            rawPosts.addAll(runHashtagSearch(hashtags));
        }

        // Fallback/complemento: busca por PERFIS do nicho (searchType=user)
        // usando os searchTerms da IA. Roda sempre que as hashtags sozinhas
        // não trouxeram volume suficiente, somando aos sinais já coletados
        // em vez de descartá-los.
        if (rawPosts.size() < MIN_SIGNALS_BEFORE_FALLBACK) {
            var searchTerms = client.getAiTerms().searchTerms() != null
                    ? client.getAiTerms().searchTerms().stream()
                            .map(t -> t == null ? "" : t.trim())
                            .filter(t -> !t.isEmpty())
                            .distinct()
                            .limit(MAX_PROFILE_FALLBACK_TERMS)
                            .toList()
                    : List.<String>of();

            for (String term : searchTerms) {
                rawPosts.addAll(runProfileSearch(term));
                if (rawPosts.size() >= MIN_SIGNALS_BEFORE_FALLBACK) break;
            }
        }

        return toSignals(rawPosts);
    }

    /** Remove '#', acentos, espaços e demais caracteres inválidos para uso em URL de hashtag. */
    private List<String> sanitizedHashtags(ClientEntity client) {
        var rawHashtags = client.getAiTerms().hashtags();
        if (rawHashtags == null) return List.of();
        return rawHashtags.stream()
                .map(this::sanitizeHashtag)
                .filter(h -> !h.isEmpty())
                .distinct()
                .limit(MAX_HASHTAGS)
                .toList();
    }

    private String sanitizeHashtag(String hashtag) {
        if (hashtag == null) return "";
        String withoutAccents = Normalizer.normalize(hashtag, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.replace("#", "").trim().toLowerCase()
                .replaceAll("[^a-z0-9_]", "");
    }

    private List<ApifyInstagramPostResponseDTO> runHashtagSearch(List<String> hashtags) {
        var directUrls = hashtags.stream().map(h -> String.format(INSTAGRAM_HASHTAG_URL, h)).toList();
        var request = ApifyInstagramRunRequestDTO.forHashtagUrls(directUrls, postsPerProfile);
        return runAndFetchPosts(request);
    }

    private List<ApifyInstagramPostResponseDTO> runProfileSearch(String searchTerm) {
        var request = ApifyInstagramRunRequestDTO.forProfileSearch(searchTerm, profilesPerSearch, postsPerProfile);
        return runAndFetchPosts(request);
    }

    private List<ApifyInstagramPostResponseDTO> runAndFetchPosts(ApifyInstagramRunRequestDTO request) {
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
        // caption o descarta.
        return apifyClient.getDatasetItems(datasetId)
                .stream()
                .filter(post -> post.caption() != null)
                .filter(post -> post.likesCount() != null || post.commentsCount() != null)
                .toList();
    }

    /**
     * Rankeia por engajamento, remove posts duplicados (podem se repetir
     * entre a busca por hashtag e o fallback por perfil) e limita posts por
     * perfil para os sinais não ficarem dominados por uma única conta.
     */
    private List<ApifyContentIdeaSignalDTO> toSignals(List<ApifyInstagramPostResponseDTO> posts) {
        var seenIds = new java.util.HashSet<String>();
        var postsPerOwner = new java.util.HashMap<String, Integer>();
        var signals = new java.util.ArrayList<ApifyContentIdeaSignalDTO>();

        posts.stream()
                .sorted((a, b) -> Double.compare(calculateScore(b), calculateScore(a)))
                .forEach(post -> {
                    if (signals.size() >= MAX_SIGNALS) return;
                    String postId = post.id() != null ? post.id() : post.shortCode();
                    if (postId != null && !seenIds.add(postId)) return;
                    String owner = post.ownerUsername() != null ? post.ownerUsername() : "";
                    int count = postsPerOwner.merge(owner, 1, Integer::sum);
                    if (count > MAX_SIGNALS_PER_OWNER) return;
                    signals.add(new ApifyContentIdeaSignalDTO(
                            post.caption(),
                            post.likesCount(),
                            post.commentsCount(),
                            post.timestamp(),
                            post.type(),
                            post.hashtags(),
                            calculateScore(post)
                    ));
                });
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
