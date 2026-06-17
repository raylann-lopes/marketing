package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.CaptionRequestDTO;
import com.north.producoes.controller.dto.request.PostRequestDTO;
import com.north.producoes.controller.dto.response.ApproveResponseDTO;
import com.north.producoes.controller.dto.response.PostResponseDTO;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.service.PostService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@AllArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<List<PostResponseDTO>> findAll() {
        List<PostResponseDTO> post = postService.findAllPost()
                .stream()
                .map(PostResponseDTO::from)
                .toList();
        return ResponseEntity.ok(post);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PostResponseDTO>> findByStatus(@PathVariable PostStatusEnum status) {
        List<PostResponseDTO> postStatus = postService.findByStatus(status)
                .stream()
                .map(PostResponseDTO::from)
                .toList();
        return ResponseEntity.ok(postStatus);
    }

    @GetMapping("/client/{id}")
    public ResponseEntity<List<PostResponseDTO>> findByClient(@PathVariable Long id) {
        List<PostResponseDTO> postClient = postService.findByClient(id)
                .stream()
                .map(PostResponseDTO::from)
                .toList();
        return ResponseEntity.ok(postClient);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<List<PostResponseDTO>> findByUser(@PathVariable Long id) {
        List<PostResponseDTO> postUser = postService.findByUser(id)
                .stream()
                .map(PostResponseDTO::from)
                .toList();
        return ResponseEntity.ok(postUser);
    }

    @GetMapping("/scheduled/{scheduledAt}")
    public ResponseEntity<List<PostResponseDTO>> findByScheduledAt(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledAtBefore) {
        List<PostResponseDTO> postScheduledAt = postService.findByScheduledAt(scheduledAt, scheduledAtBefore)
                .stream()
                .map(PostResponseDTO::from)
                .toList();
        return ResponseEntity.ok(postScheduledAt);
    }

    @PostMapping("/save")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostResponseDTO> savePost(@Valid @RequestBody PostRequestDTO post) {
        return ResponseEntity.ok(PostResponseDTO.from(postService.savePost(post)));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostResponseDTO> updatePost(@PathVariable Long id, @Valid @RequestBody PostRequestDTO post) {
        return ResponseEntity.ok(PostResponseDTO.from(postService.updatePost(id, post)));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<Void> deletePostById(@PathVariable Long id) {
        postService.deletePostById(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/update-reference/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<PostResponseDTO> updateReferenceImage(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, List<String>> body,
            @RequestParam(required = false) String s3Key) {
        
        List<String> s3Keys = body != null ? body.get("s3Keys") : null;
        if (s3Keys != null && !s3Keys.isEmpty()) {
            return ResponseEntity.ok(PostResponseDTO.from(postService.updateReferenceImage(id, s3Keys)));
        }
        if (s3Key != null && !s3Key.isEmpty()) {
            return ResponseEntity.ok(PostResponseDTO.from(postService.updateReferenceImage(id, List.of(s3Key))));
        }
        throw new IllegalArgumentException("Forneça s3Key(s) via query param ou body");
    }

    @PostMapping("/{id}/generate-caption")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApproveResponseDTO> generateCaption(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CaptionRequestDTO request) {
        String artS3Key = (request != null) ? request.artS3Key() : null;
        return ResponseEntity.ok(ApproveResponseDTO.from(postService.generateCaption(id, artS3Key)));
    }
}
