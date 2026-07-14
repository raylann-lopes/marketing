package com.north.producoes.controller.api;

import com.north.producoes.controller.dto.request.MediaUploadCompleteRequestDTO;
import com.north.producoes.controller.dto.response.MediaUploadCompleteResponseDTO;
import com.north.producoes.controller.dto.response.MediaUrlResponseDTO;
import com.north.producoes.controller.dto.response.PresignedUploadResponseDTO;
import com.north.producoes.entity.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Mídia", description = "Upload e preview de arquivos de mídia via S3")
public interface MediaApi {

    @RequestMapping(value = "/upload-url", method = {RequestMethod.GET, RequestMethod.POST})
    @Operation(summary = "Gera URL presigned para upload de arte ao S3")
    @ApiResponse(responseCode = "200", description = "URL de upload gerada com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    ResponseEntity<PresignedUploadResponseDTO> generateUploadUrl(
            @RequestParam Long postId,
            @RequestParam String filename,
            @RequestParam String contentType,
            @AuthenticationPrincipal UserEntity user);

    @RequestMapping(value = "/reference-url", method = {RequestMethod.GET, RequestMethod.POST})
    @Operation(summary = "Gera URL presigned para upload de imagem de referência ao S3")
    @ApiResponse(responseCode = "200", description = "URL de upload gerada com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    ResponseEntity<PresignedUploadResponseDTO> generateReferenceUploadUrl(
            @RequestParam Long postId,
            @RequestParam String filename,
            @RequestParam String contentType,
            @AuthenticationPrincipal UserEntity user);

    @PostMapping("/upload-complete")
    @Operation(summary = "Confirma upload e atualiza status do post para WAITING_APPROVAL")
    @ApiResponse(responseCode = "200", description = "Upload confirmado com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    ResponseEntity<MediaUploadCompleteResponseDTO> markUploadComplete(
            @Valid @RequestBody MediaUploadCompleteRequestDTO request,
            @AuthenticationPrincipal UserEntity user);

    @GetMapping("/preview/{postId}")
    @Operation(summary = "Retorna URL pública de preview da arte do post")
    @ApiResponse(responseCode = "200", description = "URL de preview gerada com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    ResponseEntity<MediaUrlResponseDTO> getArtPreviewUrl(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserEntity user);

    @GetMapping("/reference-preview/{postId}")
    @Operation(summary = "Retorna URL pública de preview da imagem de referência do post")
    @ApiResponse(responseCode = "200", description = "URL de preview gerada com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    ResponseEntity<MediaUrlResponseDTO> getReferencePreviewUrl(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserEntity user);
}
