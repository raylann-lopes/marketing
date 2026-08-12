package com.north.producoes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.north.producoes.entity.enums.TaskPriorityEnum;
import com.north.producoes.entity.enums.TaskTypeEnum;
import com.north.producoes.exception.AiIntegrationException;
import com.north.producoes.integration.openai.dto.TaskAiInterpretationDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskAiInterpreterServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    private TaskAiInterpreterService service;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        service = new TaskAiInterpreterService(chatClientBuilder, new ObjectMapper());
        ReflectionTestUtils.setField(service, "model", "gpt-test");
    }

    @Test
    void shouldInterpretTaskJson() {
        ChatClient.ChatClientRequestSpec requestSpec = org.mockito.Mockito.mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = org.mockito.Mockito.mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("""
                {"title":"Cobrar fotos","description":"Cobrar fotos do post",\
                "clientName":"Imperial","dateExpires":"2026-08-12",\
                "timeExpires":"10:00","type":"COBRANCA","priority":"NORMAL"}
                """);

        List<TaskAiInterpretationDTO> results = service.interpret(
                "Lembrar a Imperial do post às 10 horas e mandar as fotos",
                ZonedDateTime.of(2026, 8, 12, 9, 0, 0, 0, ZoneId.of("America/Sao_Paulo")),
                List.of("Imperial")
        );

        TaskAiInterpretationDTO result = results.getFirst();
        assertThat(results).hasSize(1);
        assertThat(result.title()).isEqualTo("Cobrar fotos");
        assertThat(result.dateExpires()).isEqualTo("2026-08-12");
        assertThat(result.timeExpires()).isEqualTo("10:00");
        assertThat(result.type()).isEqualTo(TaskTypeEnum.COBRANCA);
        assertThat(result.priority()).isEqualTo(TaskPriorityEnum.NORMAL);
    }

    @Test
    void shouldInterpretMultipleTasksFromArray() {
        ChatClient.ChatClientRequestSpec requestSpec = org.mockito.Mockito.mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = org.mockito.Mockito.mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("""
                [
                  {"title":"Cobrar fotos","description":null,"clientName":"Imperial",\
                  "dateExpires":"2026-08-13","timeExpires":"09:00",\
                  "type":"COBRANCA","priority":"URGENT"},
                  {"title":"Revisar calendário editorial","description":null,"clientName":null,\
                  "dateExpires":"2026-08-13","timeExpires":"11:30",\
                  "type":"TAREFA","priority":"HIGH"}
                ]
                """);

        List<TaskAiInterpretationDTO> results = service.interpret(
                "Amanhã às 9h cobrar fotos. Amanhã às 11h30 revisar o calendário.",
                ZonedDateTime.of(2026, 8, 12, 9, 0, 0, 0, ZoneId.of("America/Sao_Paulo")),
                List.of("Imperial")
        );

        assertThat(results).hasSize(2);
        assertThat(results).extracting(TaskAiInterpretationDTO::title)
                .containsExactly("Cobrar fotos", "Revisar calendário editorial");
    }

    @Test
    void shouldRejectInvalidAiResponse() {
        ChatClient.ChatClientRequestSpec requestSpec = org.mockito.Mockito.mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = org.mockito.Mockito.mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("não é json");

        assertThatThrownBy(() -> service.interpret(
                "Criar tarefa",
                ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")),
                List.of()
        ))
                .isInstanceOf(AiIntegrationException.class)
                .hasMessageContaining("interpretar a tarefa");
    }
}
