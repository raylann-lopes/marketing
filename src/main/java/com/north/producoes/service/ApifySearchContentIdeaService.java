package com.north.producoes.service;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.integration.apify.dto.ContentIdeaTermsDTO;
import com.north.producoes.repository.ClientRepository;
import org.springframework.stereotype.Service;

@Service
public class ApifySearchContentIdeaService {

    private final ContentIdeaAiService contentIdeaAiService;
    private final ClientRepository clientRepository;


    public ApifySearchContentIdeaService(ContentIdeaAiService contentIdeaAiService, ClientRepository clientRepository) {
        this.contentIdeaAiService = contentIdeaAiService;
        this.clientRepository = clientRepository;
    }

    public ContentIdeaTermsDTO searchIdeas (ClientEntity client){
        var terms = contentIdeaAiService.generateContentIdeas(client);
        client.setAiTerms(terms);
        clientRepository.save(client);
        return terms;
    }
}
