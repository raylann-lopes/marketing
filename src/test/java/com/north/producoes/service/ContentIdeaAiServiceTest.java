package com.north.producoes.service;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.exception.InvalidClientDataException;
import com.north.producoes.integration.apify.dto.ContentIdeaTermsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("ContentIdeaAiService")
@ExtendWith(MockitoExtension.class)
class ContentIdeaAiServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    private ContentIdeaAiService contentIdeaAiService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        contentIdeaAiService = new ContentIdeaAiService(chatClientBuilder);
    }

    @Nested
    @DisplayName("generateContentIdeas()")
    class GenerateContentIdeas {

        @Test
        @DisplayName("deve retornar DTO com termos quando cliente tem nicho válido")
        void shouldReturnTermsWhenClientHasNiche() throws Exception {
            ClientEntity client = clientWithNiche();
            ContentIdeaTermsDTO expected = new ContentIdeaTermsDTO(
                    List.of("odontologiaestetica", "sorrisoperfeito"),
                    List.of("clareamento dental antes e depois", "mitos sobre clareamento")
            );

            ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);

            when(chatClient.prompt(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(requestSpec);
            when(requestSpec.call()).thenReturn(callSpec);
            when(callSpec.content()).thenReturn("{\"hashtags\":[\"odontologiaestetica\",\"sorrisoperfeito\"],\"searchTerms\":[\"clareamento dental antes e depois\",\"mitos sobre clareamento\"]}");

            ContentIdeaTermsDTO result = contentIdeaAiService.generateContentIdeas(client);

            assertThat(result.hashtags()).containsExactly("odontologiaestetica", "sorrisoperfeito");
            assertThat(result.searchTerms()).containsExactly("clareamento dental antes e depois", "mitos sobre clareamento");
        }

        @Test
        @DisplayName("deve lançar InvalidClientDataException quando cliente não tem nicho")
        void shouldThrowWhenClientHasNoNiche() {
            ClientEntity client = new ClientEntity();
            client.setId(1L);
            client.setName("Cliente Sem Nicho");
            client.setNiche(null);

            assertThatThrownBy(() -> contentIdeaAiService.generateContentIdeas(client))
                    .isInstanceOf(InvalidClientDataException.class)
                    .hasMessageContaining("1");

            verifyNoInteractions(chatClient);
        }

        @Test
        @DisplayName("deve retornar aiTerms do cliente como fallback quando a IA falha")
        void shouldReturnSavedTermsWhenAiFails() {
            ContentIdeaTermsDTO fallback = new ContentIdeaTermsDTO(
                    List.of("odontologia"),
                    List.of("dentista estetico")
            );

            ClientEntity client = clientWithNiche();
            client.setAiTerms(fallback);

            ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);

            when(chatClient.prompt(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(requestSpec);
            when(requestSpec.call()).thenReturn(callSpec);
            when(callSpec.content()).thenReturn("resposta invalida da ia `backtick`");

            ContentIdeaTermsDTO result = contentIdeaAiService.generateContentIdeas(client);

            assertThat(result).isEqualTo(fallback);
        }

        @Test
        @DisplayName("deve lançar InvalidClientDataException quando nicho é string vazia")
        void shouldThrowWhenNicheIsBlank() {
            ClientEntity client = new ClientEntity();
            client.setId(2L);
            client.setName("Cliente Nicho Vazio");
            client.setNiche("   ");

            assertThatThrownBy(() -> contentIdeaAiService.generateContentIdeas(client))
                    .isInstanceOf(InvalidClientDataException.class);

            verifyNoInteractions(chatClient);
        }
    }

    @Nested
    @DisplayName("buildPrompt()")
    class BuildPrompt {

        @Test
        @DisplayName("deve incluir nome, nicho e tom de voz do cliente no prompt")
        void shouldIncludeClientDataInPrompt() {
            ClientEntity client = clientWithNiche();

            String prompt = contentIdeaAiService.buildPrompt(client);

            assertThat(prompt).contains("Clínica Dental");
            assertThat(prompt).contains("Odontologia");
            assertThat(prompt).contains("Profissional e acolhedor");
        }
    }

    private static ClientEntity clientWithNiche() {
        ClientEntity client = new ClientEntity();
        client.setId(1L);
        client.setName("Clínica Dental");
        client.setNiche("Odontologia");
        client.setVoiceTone("Profissional e acolhedor");
        return client;
    }
}
