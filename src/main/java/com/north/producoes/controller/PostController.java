package com.north.producoes.controller;

import com.north.producoes.controller.api.PostApi;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
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
    public ResponseEntity<List<PostResponseDTO>> findByClient(ClientEntity clientId) {
        List<PostResponseDTO> post = postService.findByClient(clientId)
                .stream()
                .map(PostResponseDTO::from)
                .toList();
        return ResponseEntity.ok(post);
    }

    @Override
    public ResponseEntity<List<PostResponseDTO>> findByUser(UserEntity user) {
        List<PostResponseDTO> postUser = postService.findByUser(user)
                .stream()
                .map(PostResponseDTO::from)
                .toList();
        return ResponseEntity.ok(postUser);
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
    public ResponseEntity<PostResponseDTO> savePost(PostEntity post) {
        return ResponseEntity.ok(PostResponseDTO.from(postService.savePost(post)));
    }

    @Override
    public ResponseEntity<PostResponseDTO> updatePost(PostEntity post) {
        return ResponseEntity.ok(PostResponseDTO.from(postService.updatePost(post)));
    }

    @Override
    public ResponseEntity<Void> deletePostById(Long id) {
        postService.deletePostById(id);
        return ResponseEntity.noContent().build();
    }
}
