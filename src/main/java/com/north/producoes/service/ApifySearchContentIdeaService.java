package com.north.producoes.service;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.ContentIdeaRunsEntity;
import com.north.producoes.entity.enums.ContentIdeaRunsStatusEnum;
import com.north.producoes.integration.apify.ApifyClient;
import com.north.producoes.integration.apify.dto.ApifyContentIdeaSignalDTO;
import com.north.producoes.integration.apify.dto.ApifyInstagramPostResponseDTO;
import com.north.producoes.integration.apify.dto.ApifyInstagramRunRequestDTO;
import com.north.producoes.integration.apify.dto.ContentIdeaTermsDTO;
import com.north.producoes.repository.ClientRepository;
import com.north.producoes.repository.ContentIdeaRunsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApifySearchContentIdeaService {

    private final ContentIdeaAiService contentIdeaAiService;
    private final ClientRepository clientRepository;
    private final ApifyClient apifyClient;
    private final ContentIdeaRunsRepository contentIdeaRunsRepository;

    public ApifySearchContentIdeaService(
            ContentIdeaAiService contentIdeaAiService,
            ClientRepository clientRepository,
            ApifyClient apifyClient,
            ContentIdeaRunsRepository contentIdeaRunsRepository
    ) {
        this.contentIdeaAiService = contentIdeaAiService;
        this.clientRepository = clientRepository;
        this.apifyClient = apifyClient;
        this.contentIdeaRunsRepository = contentIdeaRunsRepository;
    }

    public ContentIdeaTermsDTO generateIdeas(ClientEntity client) {
        var terms = contentIdeaAiService.generateContentIdeas(client);
        client.setAiTerms(terms);
        clientRepository.save(client);
        return terms;
    }

    public List<ApifyContentIdeaSignalDTO> searchInstagramSignals(ClientEntity client) {
        var terms = client.getAiTerms().hashtags()
                .stream()
                .map(h -> h.replace("#", ""))
                .toList();

        ContentIdeaRunsEntity run = new ContentIdeaRunsEntity();
        run.setClient(client);
        run.setNiche(client.getNiche());
        run.setStatus(ContentIdeaRunsStatusEnum.STARTED);
        contentIdeaRunsRepository.save(run);

        var request = new ApifyInstagramRunRequestDTO(terms, "hashtag", "posts", 10);
        var runResponse = apifyClient.runInstagramScraper(request);
        var apifyRunId = runResponse.data().id();
        var apifyStatus = "RUNNING";

        while (apifyStatus.equals("RUNNING")) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Polling interrompido.", e);
            }
            var poll = apifyClient.getRun(apifyRunId);
            apifyStatus = poll.data().status();

            if (apifyStatus.equals("FAILED")) {
                run.setStatus(ContentIdeaRunsStatusEnum.FAILED);
                run.setErrorMessage("Execucao do Actor falhou na Apify.");
                contentIdeaRunsRepository.save(run);
                throw new RuntimeException("Execucao do Apify falhou.");
            }
        }

        var datasetId = runResponse.data().defaultDatasetId();
        var signals = apifyClient.getDatasetItems(datasetId)
                .stream()
                .filter(post -> post.caption() != null)
                .filter(post -> post.likesCount() != null || post.commentsCount() != null)
                .map(post -> new ApifyContentIdeaSignalDTO(
                        post.caption(),
                        post.likesCount(),
                        post.commentsCount(),
                        post.timestamp(),
                        post.type(),
                        post.hashtags(),
                        calcularScore(post)
                ))
                .toList();

        run.setStatus(ContentIdeaRunsStatusEnum.SUCESS);
        run.setIdeasCount(signals.size());
        contentIdeaRunsRepository.save(run);

        return signals;
    }

    private Double calcularScore(ApifyInstagramPostResponseDTO post) {
        int likes = post.likesCount() != null ? post.likesCount() : 0;
        int comments = post.commentsCount() != null ? post.commentsCount() : 0;
        return (double) (likes + (comments * 2));
    }
}
