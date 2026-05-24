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

@RestController
@RequestMapping("/api/media")
@AllArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @RequestMapping(value = "/upload-url", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<PresignedUploadResponseDTO> generateUploadUrl(
            @RequestParam Long postId,
            @RequestParam String filename,
            @RequestParam String contentType,
            @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(mediaService.generateUploadUrl(postId, filename, contentType, user));
    }

    @RequestMapping(value = "/reference-url", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<PresignedUploadResponseDTO> generateReferenceUploadUrl(
            @RequestParam Long postId,
            @RequestParam String filename,
            @RequestParam String contentType,
            @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(mediaService.generateReferenceUploadUrl(postId, filename, contentType, user));
    }

    @PostMapping("/upload-complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MediaUploadCompleteResponseDTO> markUploadComplete(
            @Valid @RequestBody MediaUploadCompleteRequestDTO request,
            @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(mediaService.markUploadComplete(request, user));
    }

    @GetMapping("/preview/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MediaUrlResponseDTO> getArtPreviewUrl(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(mediaService.getArtPreviewUrl(postId, user));
    }

    @GetMapping("/reference-preview/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MediaUrlResponseDTO> getReferencePreviewUrl(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(mediaService.getReferencePreviewUrl(postId, user));
    }
}
