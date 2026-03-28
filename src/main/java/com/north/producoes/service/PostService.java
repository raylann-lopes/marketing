package com.north.producoes.service;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class PostService {
    private final PostRepository  postRepository;

    public List<PostEntity> findAllPost(){
        return postRepository.findAll();
    }

    public List<PostEntity> findByStatus(PostStatusEnum status){
        List<PostEntity> postStatus = postRepository.findByStatus(status);
        if(postRepository.findByStatus(status).isEmpty()){
            throw new RuntimeException("Post nao encontrado");
        }
        return postStatus;
    }


    public List<PostEntity> findByClient(ClientEntity client){
        List<PostEntity> postClient = postRepository.findByClient(client);
        if(postRepository.findByClient(client).isEmpty()){
            throw new RuntimeException("Post nao encontrado");
        }
        return postClient;
    }

    public List<PostEntity> findByUser(UserEntity user){
        List<PostEntity> postUser = postRepository.findByUser(user);
        if(postRepository.findByUser(user).isEmpty()){
            throw new RuntimeException("Post nao encontrado");
        }
        return postUser;
    }

    public List<PostEntity> findByScheduledAt(LocalDateTime scheduledAtAfter, LocalDateTime scheduledAtBefore){
        List<PostEntity> postScheduledAt = postRepository.findByScheduledAtBetween(scheduledAtAfter, scheduledAtBefore);
        if(postRepository.findByScheduledAtBetween(scheduledAtAfter, scheduledAtBefore).isEmpty()){
            throw new RuntimeException("Post nao encontrado");
        } else if (scheduledAtAfter.isAfter(scheduledAtBefore)){
            throw new RuntimeException("Data de inicio deve ser anterior a data de fim");
        }
        return postScheduledAt;
    }

    @Transactional
    public PostEntity savePost(PostEntity post){
        return postRepository.save(post);
    }

    @Transactional
    public PostEntity updatePost(PostEntity post){
        if(postRepository.findById(post.getId()).isEmpty()){
            throw new RuntimeException("Post nao encontrado");
        }
        return postRepository.save(post);
    }

    @Transactional
    public void deletePostById(Long id){
        if(postRepository.findById(id).isEmpty()){
            throw new RuntimeException("Post nao encontrado");
        }
        postRepository.deleteById(id);
    }

}
