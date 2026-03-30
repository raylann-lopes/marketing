package com.north.producoes.controller;

import com.north.producoes.controller.api.PostApi;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.dto.response.PostResponse;
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
    public ResponseEntity<List<PostResponse>> findAll() {
        List<PostResponse> post = postService.findAllPost()
                .stream()
                .map(PostResponse::from)
                .toList();
        return ResponseEntity.ok(post);
    }

    @Override
    public ResponseEntity<List<PostResponse>> findByStatus(PostStatusEnum status) {
        List<PostResponse> postStatus = postService.findByStatus(status)
                .stream()
                .map(PostResponse::from)
                .toList();
        return ResponseEntity.ok(postStatus);
    }

    @Override
    public ResponseEntity<List<PostResponse>> findByClient(ClientEntity clientId) {
        List<PostResponse> post = postService.findByClient(clientId)
                .stream()
                .map(PostResponse::from)
                .toList();
        return ResponseEntity.ok(post);
    }

    @Override
    public ResponseEntity<List<PostResponse>> findByUser(UserEntity user) {
        List<PostResponse> postUser = postService.findByUser(user)
                .stream()
                .map(PostResponse::from)
                .toList();
        return ResponseEntity.ok(postUser);
    }

    @Override
    public ResponseEntity<List<PostResponse>> findByScheduledAt(LocalDateTime scheduledAt, LocalDateTime scheduledAtBefore) {
        List<PostResponse> postScheduledAt = postService.findByScheduledAt(scheduledAt, scheduledAtBefore)
                .stream()
                .map(PostResponse::from)
                .toList();
        return ResponseEntity.ok(postScheduledAt);
    }

    @Override
    public ResponseEntity<PostResponse> savePost(PostEntity post) {
        return ResponseEntity.ok(PostResponse.from(postService.savePost(post)));
    }

    @Override
    public ResponseEntity<PostResponse> updatePost(PostEntity post) {
        return ResponseEntity.ok(PostResponse.from(postService.updatePost(post)));
    }

    @Override
    public ResponseEntity<Void> deletePostById(Long id) {
        postService.deletePostById(id);
        return ResponseEntity.noContent().build();
    }
}
