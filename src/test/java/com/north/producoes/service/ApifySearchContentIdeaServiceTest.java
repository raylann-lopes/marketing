package com.north.producoes.service;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.exception.AiIntegrationException;
import com.north.producoes.integration.apify.ApifyClient;
import com.north.producoes.integration.apify.dto.ApifyInstagramPostResponseDTO;
import com.north.producoes.integration.apify.dto.ApifyInstagramRunRequestDTO;
import com.north.producoes.integration.apify.dto.ApifyRunDataDTO;
import com.north.producoes.integration.apify.dto.ApifyRunResponseDTO;
import com.north.producoes.integration.apify.dto.ContentIdeaTermsDTO;
import com.north.producoes.repository.ClientRepository;
import com.north.producoes.repository.ContentIdeaRunsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("ApifySearchContentIdeaService")
@ExtendWith(MockitoExtension.class)
class ApifySearchContentIdeaServiceTest {

    @Mock
    private ContentIdeaAiService contentIdeaAiService;

    @Mock
    private ContentIdeaService contentIdeaService;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ApifyClient apifyClient;

    @Mock
    private ContentIdeaRunsRepository contentIdeaRunsRepository;

    // Construção manual: o construtor tem parâmetros int (config de custo)
    // que o @InjectMocks não consegue fornecer
    private ApifySearchContentIdeaService apifySearchContentIdeaService;

    @BeforeEach
    void setUp() {
        apifySearchContentIdeaService = new ApifySearchContentIdeaService(
                contentIdeaAiService, contentIdeaService, clientRepository,
                apifyClient, contentIdeaRunsRepository, 3, 8);
    }

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

        @Test
        @DisplayName("deve lançar AiIntegrationException quando a IA não retorna termos")
        void shouldThrowWhenAiReturnsNoTerms() {
            ClientEntity client = new ClientEntity();
            client.setId(3L);
            client.setNiche("Odontologia");

            when(contentIdeaAiService.generateContentIdeas(client)).thenReturn(null);

            assertThatThrownBy(() -> apifySearchContentIdeaService.generateIdeas(client))
                    .isInstanceOf(AiIntegrationException.class);
            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar AiIntegrationException quando termos vêm vazios")
        void shouldThrowWhenTermsAreEmpty() {
            ClientEntity client = new ClientEntity();
            client.setId(4L);
            client.setNiche("Fitness");

            when(contentIdeaAiService.generateContentIdeas(client))
                    .thenReturn(new ContentIdeaTermsDTO(List.of(), List.of()));

            assertThatThrownBy(() -> apifySearchContentIdeaService.generateIdeas(client))
                    .isInstanceOf(AiIntegrationException.class);
        }
    }

    @Nested
    @DisplayName("searchInstagramSignals() / coleta de sinais")
    class CollectSignals {

        private ClientEntity clientWithTerms(List<String> hashtags, List<String> searchTerms) {
            ClientEntity client = new ClientEntity();
            client.setId(10L);
            client.setNiche("Odontologia");
            client.setAiTerms(new ContentIdeaTermsDTO(hashtags, searchTerms));
            return client;
        }

        private ApifyRunResponseDTO runResponse(String status) {
            return new ApifyRunResponseDTO(new ApifyRunDataDTO("run-1", status, "dataset-1"));
        }

        private ApifyInstagramPostResponseDTO post(String owner, int likes, int comments) {
            return new ApifyInstagramPostResponseDTO(
                    "id", "Image", "shortcode", "legenda do post", List.of(), List.of(),
                    "url", comments, likes, null, null, "2026-01-01T00:00:00Z", owner, owner);
        }

        @Test
        @DisplayName("deve buscar por hashtag via directUrls quando o cliente tem hashtags")
        void shouldSearchByHashtagUrls() {
            ClientEntity client = clientWithTerms(
                    List.of("#Odontologia Estética!", "clareamento"), List.of());
            when(contentIdeaRunsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(apifyClient.runInstagramScraper(any())).thenReturn(runResponse("SUCCEEDED"));
            when(apifyClient.getRun("run-1")).thenReturn(runResponse("SUCCEEDED"));
            when(apifyClient.getDatasetItems("dataset-1"))
                    .thenReturn(List.of(post("perfil_nicho", 100, 10)));

            List<?> signals = apifySearchContentIdeaService.searchInstagramSignals(client);

            assertThat(signals).hasSize(1);
            ArgumentCaptor<ApifyInstagramRunRequestDTO> captor =
                    ArgumentCaptor.forClass(ApifyInstagramRunRequestDTO.class);
            verify(apifyClient, times(1)).runInstagramScraper(captor.capture());
            ApifyInstagramRunRequestDTO sent = captor.getValue();
            assertThat(sent.search()).isNull();
            assertThat(sent.searchType()).isNull();
            assertThat(sent.directUrls()).containsExactlyInAnyOrder(
                    "https://www.instagram.com/explore/tags/odontologiaestetica/",
                    "https://www.instagram.com/explore/tags/clareamento/");
        }

        @Test
        @DisplayName("deve cair no fallback de busca por perfil quando hashtag não retorna sinais")
        void shouldFallBackToProfileSearchWhenHashtagIsEmpty() {
            ClientEntity client = clientWithTerms(List.of("nichoraro"), List.of("perfil do nicho"));
            when(contentIdeaRunsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(apifyClient.runInstagramScraper(any())).thenReturn(runResponse("SUCCEEDED"));
            when(apifyClient.getRun("run-1")).thenReturn(runResponse("SUCCEEDED"));
            // Primeira chamada (hashtag) vazia, segunda (perfil) com resultado
            when(apifyClient.getDatasetItems("dataset-1"))
                    .thenReturn(List.of())
                    .thenReturn(List.of(post("perfil_nicho", 50, 5)));

            List<?> signals = apifySearchContentIdeaService.searchInstagramSignals(client);

            assertThat(signals).hasSize(1);
            ArgumentCaptor<ApifyInstagramRunRequestDTO> captor =
                    ArgumentCaptor.forClass(ApifyInstagramRunRequestDTO.class);
            verify(apifyClient, times(2)).runInstagramScraper(captor.capture());
            ApifyInstagramRunRequestDTO hashtagCall = captor.getAllValues().get(0);
            ApifyInstagramRunRequestDTO profileCall = captor.getAllValues().get(1);
            assertThat(hashtagCall.directUrls()).isNotEmpty();
            assertThat(profileCall.searchType()).isEqualTo("user");
            assertThat(profileCall.search()).isEqualTo("perfil do nicho");
        }

        @Test
        @DisplayName("deve ir direto pro fallback de perfil quando cliente não tem hashtags")
        void shouldSkipHashtagWhenNoneConfigured() {
            ClientEntity client = clientWithTerms(List.of(), List.of("perfil do nicho"));
            when(contentIdeaRunsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(apifyClient.runInstagramScraper(any())).thenReturn(runResponse("SUCCEEDED"));
            when(apifyClient.getRun("run-1")).thenReturn(runResponse("SUCCEEDED"));
            when(apifyClient.getDatasetItems("dataset-1"))
                    .thenReturn(List.of(post("perfil_nicho", 50, 5)));

            apifySearchContentIdeaService.searchInstagramSignals(client);

            ArgumentCaptor<ApifyInstagramRunRequestDTO> captor =
                    ArgumentCaptor.forClass(ApifyInstagramRunRequestDTO.class);
            verify(apifyClient, times(1)).runInstagramScraper(captor.capture());
            assertThat(captor.getValue().searchType()).isEqualTo("user");
        }
    }
}
