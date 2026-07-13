package com.north.producoes.service;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.ContentIdeaEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ContentIdeaPriorityEnum;
import com.north.producoes.entity.enums.ContentIdeaStatusEnum;
import com.north.producoes.entity.enums.ContentIdeiaFormatEnum;
import com.north.producoes.entity.enums.PostFormatEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.integration.apify.dto.ApifyContentIdeaSignalDTO;
import com.north.producoes.integration.apify.dto.ContentIdeaAiResponseDTO;
import com.north.producoes.repository.ContentIdeaRepository;
import com.north.producoes.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Geração e persistência de ideias de conteúdo")
class ContentIdeaServiceTest {

    @Mock private ContentIdeaRepository contentIdeaRepository;
    @Mock private ContentIdeaAiService contentIdeaAiService;
    @Mock private PostRepository postRepository;

    @InjectMocks private ContentIdeaService contentIdeaService;

    private ClientEntity client;
    private List<ApifyContentIdeaSignalDTO> signals;

    @BeforeEach
    void setUp() {
        client = new ClientEntity();
        client.setId(1L);
        client.setName("Clínica Aurora");
        client.setNiche("Saúde e estética");
        client.setVoiceTone("Formal");

        signals = List.of(
                new ApifyContentIdeaSignalDTO(
                        "Legenda viral", 1000, 50, "2026-07-01", "Image",
                        List.of("#estetica"), 1100.0),
                new ApifyContentIdeaSignalDTO(
                        "Legenda menor", 200, 10, "2026-07-02", "Video",
                        List.of("#skincare"), 220.0));
    }

    @Test
    @DisplayName("ideia nova é persistida com status inicial SUGGESTED")
    void persisteIdeiaComStatusSuggested() {
        when(contentIdeaAiService.generateIdeasFromSignals(any(), any(), anyInt()))
                .thenReturn(new ContentIdeaAiResponseDTO(List.of(
                        new ContentIdeaAiResponseDTO.GeneratedIdea(
                                "Título novo", "Gancho", "Tema", "Objetivo",
                                "REELS", "Justificativa", "HIGH", "Resumo do sinal", 1))));
        when(contentIdeaRepository.existsByClientIdAndTitleIgnoreCaseAndCreatedAtAfter(
                anyLong(), anyString(), any(LocalDateTime.class))).thenReturn(false);
        when(contentIdeaRepository.save(any(ContentIdeaEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<ContentIdeaEntity> result = contentIdeaService.generateAndPersistIdeas(client, signals);

        assertThat(result).hasSize(1);
        ContentIdeaEntity idea = result.getFirst();
        assertThat(idea.getStatus()).isEqualTo(ContentIdeaStatusEnum.SUGGESTED);
        assertThat(idea.getFormat()).isEqualTo(ContentIdeiaFormatEnum.REELS);
        assertThat(idea.getPriority()).isEqualTo(ContentIdeaPriorityEnum.HIGH);
        assertThat(idea.getEngagementScore()).isEqualTo(1100.0);
    }

    @Test
    @DisplayName("ideia com título duplicado recente é ignorada")
    void ignoraIdeiaDuplicadaRecente() {
        when(contentIdeaAiService.generateIdeasFromSignals(any(), any(), anyInt()))
                .thenReturn(new ContentIdeaAiResponseDTO(List.of(
                        new ContentIdeaAiResponseDTO.GeneratedIdea(
                                "Título repetido", "Gancho", "Tema", "Objetivo",
                                "FEED", "Justificativa", "MEDIUM", "Resumo", 1),
                        new ContentIdeaAiResponseDTO.GeneratedIdea(
                                "Título inédito", "Gancho 2", "Tema 2", "Objetivo 2",
                                "CAROUSEL", "Justificativa 2", "LOW", "Resumo 2", 2))));
        when(contentIdeaRepository.existsByClientIdAndTitleIgnoreCaseAndCreatedAtAfter(
                eq(1L), eq("Título repetido"), any(LocalDateTime.class))).thenReturn(true);
        when(contentIdeaRepository.existsByClientIdAndTitleIgnoreCaseAndCreatedAtAfter(
                eq(1L), eq("Título inédito"), any(LocalDateTime.class))).thenReturn(false);
        when(contentIdeaRepository.save(any(ContentIdeaEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<ContentIdeaEntity> result = contentIdeaService.generateAndPersistIdeas(client, signals);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTitle()).isEqualTo("Título inédito");
    }

    @Test
    @DisplayName("nunca persiste mais que o limite de ideias por execução")
    void respeitaLimiteMaximoDeIdeias() {
        List<ContentIdeaAiResponseDTO.GeneratedIdea> seteIdeias = java.util.stream.IntStream
                .rangeClosed(1, 7)
                .mapToObj(i -> new ContentIdeaAiResponseDTO.GeneratedIdea(
                        "Título " + i, "Gancho", "Tema", "Objetivo",
                        "FEED", "Justificativa", "MEDIUM", "Resumo", 1))
                .toList();
        when(contentIdeaAiService.generateIdeasFromSignals(any(), any(), anyInt()))
                .thenReturn(new ContentIdeaAiResponseDTO(seteIdeias));
        when(contentIdeaRepository.existsByClientIdAndTitleIgnoreCaseAndCreatedAtAfter(
                anyLong(), anyString(), any(LocalDateTime.class))).thenReturn(false);
        when(contentIdeaRepository.save(any(ContentIdeaEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<ContentIdeaEntity> result = contentIdeaService.generateAndPersistIdeas(client, signals);

        assertThat(result).hasSize(ContentIdeaService.MAX_IDEAS_PER_RUN);
    }

    @Test
    @DisplayName("formato e prioridade inválidos caem nos padrões FEED e MEDIUM")
    void formatoEPrioridadeInvalidosUsamPadrao() {
        when(contentIdeaAiService.generateIdeasFromSignals(any(), any(), anyInt()))
                .thenReturn(new ContentIdeaAiResponseDTO(List.of(
                        new ContentIdeaAiResponseDTO.GeneratedIdea(
                                "Título", "Gancho", "Tema", "Objetivo",
                                "TIKTOK", "Justificativa", "URGENTE", "Resumo", 1))));
        when(contentIdeaRepository.existsByClientIdAndTitleIgnoreCaseAndCreatedAtAfter(
                anyLong(), anyString(), any(LocalDateTime.class))).thenReturn(false);
        when(contentIdeaRepository.save(any(ContentIdeaEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<ContentIdeaEntity> result = contentIdeaService.generateAndPersistIdeas(client, signals);

        assertThat(result.getFirst().getFormat()).isEqualTo(ContentIdeiaFormatEnum.FEED);
        assertThat(result.getFirst().getPriority()).isEqualTo(ContentIdeaPriorityEnum.MEDIUM);
    }

    @Test
    @DisplayName("cada ideia recebe o score do sinal que a inspirou (signalIndex)")
    void scorePorSinalUsadoNaIdeia() {
        when(contentIdeaAiService.generateIdeasFromSignals(any(), any(), anyInt()))
                .thenReturn(new ContentIdeaAiResponseDTO(List.of(
                        new ContentIdeaAiResponseDTO.GeneratedIdea(
                                "Ideia do sinal forte", "G", "T", "O",
                                "REELS", "J", "HIGH", "R", 1),
                        new ContentIdeaAiResponseDTO.GeneratedIdea(
                                "Ideia do sinal fraco", "G", "T", "O",
                                "FEED", "J", "LOW", "R", 2),
                        new ContentIdeaAiResponseDTO.GeneratedIdea(
                                "Ideia com índice inválido", "G", "T", "O",
                                "FEED", "J", "MEDIUM", "R", 99))));
        when(contentIdeaRepository.existsByClientIdAndTitleIgnoreCaseAndCreatedAtAfter(
                anyLong(), anyString(), any(LocalDateTime.class))).thenReturn(false);
        when(contentIdeaRepository.save(any(ContentIdeaEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<ContentIdeaEntity> result = contentIdeaService.generateAndPersistIdeas(client, signals);

        assertThat(result).hasSize(3);
        // Sinais ordenados por score: 1 → 1100.0, 2 → 220.0
        assertThat(result.get(0).getEngagementScore()).isEqualTo(1100.0);
        assertThat(result.get(1).getEngagementScore()).isEqualTo(220.0);
        // Índice inválido cai no melhor score do lote
        assertThat(result.get(2).getEngagementScore()).isEqualTo(1100.0);
    }

    @Test
    @DisplayName("resposta vazia da IA não persiste nada")
    void respostaVaziaNaoPersiste() {
        when(contentIdeaAiService.generateIdeasFromSignals(any(), any(), anyInt()))
                .thenReturn(new ContentIdeaAiResponseDTO(List.of()));

        List<ContentIdeaEntity> result = contentIdeaService.generateAndPersistIdeas(client, signals);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("títulos repetidos dentro do mesmo lote da IA são deduplicados")
    void dedupDentroDoMesmoLote() {
        when(contentIdeaAiService.generateIdeasFromSignals(any(), any(), anyInt()))
                .thenReturn(new ContentIdeaAiResponseDTO(List.of(
                        new ContentIdeaAiResponseDTO.GeneratedIdea(
                                "Mesmo Título", "G", "T", "O", "FEED", "J", "MEDIUM", "R", 1),
                        new ContentIdeaAiResponseDTO.GeneratedIdea(
                                "MESMO TÍTULO", "G2", "T2", "O2", "REELS", "J2", "HIGH", "R2", 2))));
        when(contentIdeaRepository.existsByClientIdAndTitleIgnoreCaseAndCreatedAtAfter(
                anyLong(), anyString(), any(LocalDateTime.class))).thenReturn(false);
        when(contentIdeaRepository.save(any(ContentIdeaEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<ContentIdeaEntity> result = contentIdeaService.generateAndPersistIdeas(client, signals);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTitle()).isEqualTo("Mesmo Título");
    }

    @Test
    @DisplayName("converter ideia cria demanda DEMAND no board vinculada ao cliente")
    void converterCriaDemandaNoBoard() {
        UserEntity user = new UserEntity();
        user.setId(10L);

        ContentIdeaEntity idea = new ContentIdeaEntity();
        idea.setId(7L);
        idea.setClient(client);
        idea.setTitle("Dicas Rápidas de Manutenção");
        idea.setTheme("Dicas de tecnologia");
        idea.setObjective("Engajar e educar o público");
        idea.setFormat(ContentIdeiaFormatEnum.CAROUSEL);

        when(contentIdeaRepository.findByIdWithClient(7L)).thenReturn(java.util.Optional.of(idea));
        when(postRepository.save(any(PostEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contentIdeaRepository.save(any(ContentIdeaEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var response = contentIdeaService.updateStatus(7L, ContentIdeaStatusEnum.CONVERTED, user);

        assertThat(response.status()).isEqualTo(ContentIdeaStatusEnum.CONVERTED);
        assertThat(idea.getConvertedPost()).isNotNull();
        assertThat(idea.getConvertedPost().getStatus()).isEqualTo(PostStatusEnum.DEMAND);
        assertThat(idea.getConvertedPost().getTitle()).isEqualTo("Dicas Rápidas de Manutenção");
        assertThat(idea.getConvertedPost().getClient()).isEqualTo(client);
        assertThat(idea.getConvertedPost().getUser()).isEqualTo(user);
        assertThat(idea.getConvertedPost().getFormat()).isEqualTo(PostFormatEnum.CAROUSEL);
        assertThat(idea.getConvertedPost().getScheduledAt()).isNotNull();
    }

    @Test
    @DisplayName("converter duas vezes não cria demanda duplicada")
    void converterDuasVezesNaoDuplicaDemanda() {
        UserEntity user = new UserEntity();
        user.setId(10L);

        PostEntity existingPost = new PostEntity();
        existingPost.setId(99L);

        ContentIdeaEntity idea = new ContentIdeaEntity();
        idea.setId(7L);
        idea.setClient(client);
        idea.setTitle("Já convertida");
        idea.setConvertedPost(existingPost);

        when(contentIdeaRepository.findByIdWithClient(7L)).thenReturn(java.util.Optional.of(idea));
        when(contentIdeaRepository.save(any(ContentIdeaEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        contentIdeaService.updateStatus(7L, ContentIdeaStatusEnum.CONVERTED, user);

        verify(postRepository, never()).save(any(PostEntity.class));
        assertThat(idea.getConvertedPost()).isEqualTo(existingPost);
    }
}
