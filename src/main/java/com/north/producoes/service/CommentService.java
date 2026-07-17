package com.north.producoes.service;

import com.north.producoes.controller.dto.request.CommentRequestDTO;
import com.north.producoes.controller.dto.response.CommentCountResponseDTO;
import com.north.producoes.entity.CommentEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.CommentRepository;
import com.north.producoes.repository.PostRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }


    public List<CommentEntity> findCommentByPostAndCreatedAtAsc(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    public List<CommentCountResponseDTO> findCommentCounts() {
        return commentRepository.countGroupedByPost();
    }

    public CommentEntity saveComment(CommentRequestDTO dto, UserEntity currentUser) {
        PostEntity post = postRepository.findById(dto.postId())
                .orElseThrow(() -> new ResourceNotFoundException("Post não encontrado: " + dto.postId()));

        CommentEntity comment = new CommentEntity();
        comment.setPost(post);
        comment.setUser(currentUser);
        comment.setText(dto.text());
        return commentRepository.save(comment);
    }
}
