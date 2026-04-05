package com.north.producoes.controller;

import com.north.producoes.controller.api.PostApi;
import com.north.producoes.controller.dto.request.PostRequestDTO;
import com.north.producoes.controller.dto.response.PostResponseDTO;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.service.PostService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@AllArgsConstructor
public class PostController implements PostApi {

    private final PostService postService;

    @Override
    public ResponseEntity<List<PostResponseDTO>> findAll() {
        List<PostResponseDTO> post = postService.findAllPost()
                .stream()
                .map(PostResponseDTO::from)
                .toList();
        return ResponseEntity.ok(post);
    }

    @Override
    public ResponseEntity<List<PostResponseDTO>> findByStatus(PostStatusEnum status) {
        List<PostResponseDTO> postStatus = postService.findByStatus(status)
                .stream()
                .map(PostResponseDTO::from)
                .toList();
        return ResponseEntity.ok(postStatus);
    }

    @Override
    public ResponseEntity<PostResponseDTO> findByClient(Long id) {
        return ResponseEntity.ok(PostResponseDTO.from(postService.findByClient(id).getFirst()));
    }

    @Override
    public ResponseEntity<PostResponseDTO> findByUser(Long id) {
        return ResponseEntity.ok(PostResponseDTO.from(postService.findByUser(id)));
    }

    @Override
    public ResponseEntity<List<PostResponseDTO>> findByScheduledAt(LocalDateTime scheduledAt, LocalDateTime scheduledAtBefore) {
        List<PostResponseDTO> postScheduledAt = postService.findByScheduledAt(scheduledAt, scheduledAtBefore)
                .stream()
                .map(PostResponseDTO::from)
                .toList();
        return ResponseEntity.ok(postScheduledAt);
    }

    @Override
    public ResponseEntity<PostResponseDTO> savePost(PostRequestDTO post) {
        return ResponseEntity.ok(PostResponseDTO.from(postService.savePost(post)));
    }

    @Override
    public ResponseEntity<PostResponseDTO> updatePost(Long id, PostRequestDTO post) {
        return ResponseEntity.ok(PostResponseDTO.from(postService.updatePost(id, post)));
    }

    @Override
    public ResponseEntity<?> deletePostById(Long id) {
        postService.deletePostById(id);
        return ResponseEntity.ok().body("Registro deletado com sucesso!");
    }
}
