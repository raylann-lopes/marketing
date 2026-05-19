package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.MediaUploadCompleteRequestDTO;
import com.north.producoes.controller.dto.response.MediaUrlResponseDTO;
import com.north.producoes.controller.dto.response.MediaUploadCompleteResponseDTO;
import com.north.producoes.controller.dto.response.PresignedUploadResponseDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.service.MediaService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/api/media/upload-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PresignedUploadResponseDTO> generateUploadUrl(
            @RequestParam Long postId,
            @RequestParam String filename,
            @RequestParam String contentType,
            @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(mediaService.generateUploadUrl(postId, filename, contentType, user));
    }

    @PostMapping("/api/media/upload-complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MediaUploadCompleteResponseDTO> markUploadComplete(
            @Valid @RequestBody MediaUploadCompleteRequestDTO request,
            @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(mediaService.markUploadComplete(request, user));
    }

    @GetMapping("/api/media/art-url")
    public ResponseEntity<MediaUrlResponseDTO> getArtPreviewUrl(@RequestParam Long postId,
                                                                @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(mediaService.getArtPreviewUrl(postId, user));
    }

    @GetMapping("/api/internal/media-url/{postId}")
    public ResponseEntity<MediaUrlResponseDTO> getMediaUrlForN8n(@PathVariable Long postId) {
        return ResponseEntity.ok(mediaService.getMediaUrlForN8n(postId));
    }
}
