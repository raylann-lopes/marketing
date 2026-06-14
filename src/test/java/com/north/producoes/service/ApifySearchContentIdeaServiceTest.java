package com.north.producoes.service;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.integration.apify.dto.ContentIdeaTermsDTO;
import com.north.producoes.repository.ClientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("ApifySearchContentIdeaService")
@ExtendWith(MockitoExtension.class)
class ApifySearchContentIdeaServiceTest {

    @Mock
    private ContentIdeaAiService contentIdeaAiService;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ApifySearchContentIdeaService apifySearchContentIdeaService;

    @Nested
    @DisplayName("searchIdeas()")
    class SearchIdeas {

        @Test
        @DisplayName("deve gerar termos, salvar no cliente e retornar o DTO")
        void shouldGenerateSaveAndReturnTerms() {
            ClientEntity client = new ClientEntity();
            client.setId(1L);
            client.setNiche("Odontologia");

            ContentIdeaTermsDTO terms = new ContentIdeaTermsDTO(
                    List.of("odontologiaestetica"),
                    List.of("clareamento dental")
            );

            when(contentIdeaAiService.generateContentIdeas(client)).thenReturn(terms);
            when(clientRepository.save(client)).thenReturn(client);

            ContentIdeaTermsDTO result = apifySearchContentIdeaService.generateIdeas(client);

            assertThat(result).isEqualTo(terms);
            assertThat(client.getAiTerms()).isEqualTo(terms);
            verify(clientRepository).save(client);
        }

        @Test
        @DisplayName("deve persistir os termos gerados no campo aiTerms do cliente")
        void shouldPersistAiTermsOnClient() {
            ClientEntity client = new ClientEntity();
            client.setId(2L);
            client.setNiche("Fitness");

            ContentIdeaTermsDTO terms = new ContentIdeaTermsDTO(
                    List.of("fitness", "treino"),
                    List.of("treino em casa para iniciantes")
            );

            when(contentIdeaAiService.generateContentIdeas(client)).thenReturn(terms);

            apifySearchContentIdeaService.generateIdeas(client);

            verify(clientRepository).save(argThat(saved -> saved.getAiTerms().equals(terms)));
        }
    }
}
