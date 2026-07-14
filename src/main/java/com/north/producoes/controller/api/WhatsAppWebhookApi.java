package com.north.producoes.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "WhatsApp Webhook", description = "Recebe eventos da Evolution API via webhook para processar aprovações de posts via WhatsApp")
public interface WhatsAppWebhookApi {

    @PostMapping({"/{secret}", "/{secret}/messages-upsert"})
    @Operation(
        summary = "Recebe evento de mensagem da Evolution API",
        description = """
            Endpoint chamado pela Evolution API ao receber mensagens no WhatsApp.
            Processa votos em enquetes (pollUpdateMessage) para aprovar ou rejeitar posts,
            e respostas citadas como fallback.
            O secret é passado na URL e deve corresponder à variável EVOLUTION_WEBHOOK_SECRET.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evento processado ou ignorado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Secret inválido ou ausente"),
        @ApiResponse(responseCode = "503", description = "Webhook secret não configurado no servidor")
    })
    ResponseEntity<Void> handleWebhook(
        @Parameter(description = "Secret de autenticação embutido na URL pelo Evolution API", required = true)
        @PathVariable String secret,
        @RequestBody String rawBody
    );
}
