package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.CommentRequestDTO;
import com.north.producoes.controller.dto.response.CommentCountResponseDTO;
import com.north.producoes.controller.dto.response.CommentResponseDTO;
import com.north.producoes.entity.CommentEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@PreAuthorize("isAuthenticated()")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/counts")
    public ResponseEntity<List<CommentCountResponseDTO>> getCommentCounts() {
        return ResponseEntity.ok(commentService.findCommentCounts());
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<CommentResponseDTO>> getCommentsByPostId(@PathVariable Long postId) {
        List<CommentEntity> comments = commentService.findCommentByPostAndCreatedAtAsc(postId);
        List<CommentResponseDTO> response = comments.stream()
                .map(comment -> CommentResponseDTO.from(comment, comment.getUser()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/save")
    public ResponseEntity<CommentResponseDTO> save(@RequestBody @Valid CommentRequestDTO request, @AuthenticationPrincipal UserEntity currentUser) {
        CommentEntity saved = commentService.saveComment(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentResponseDTO.from(saved, currentUser));
    }
}
