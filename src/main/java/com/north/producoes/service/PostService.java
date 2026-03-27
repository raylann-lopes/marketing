package com.north.producoes.service;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.repository.PostRepository;
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
        if(postRepository.findByStatus(status.name()).isEmpty()){
            throw new RuntimeException("Post nao encontrado");
        }
        return postRepository.findByStatus(status.name());
    }

    public List<PostEntity> findByClient(ClientEntity client){
        if(postRepository.findByClient(client).isEmpty()){
            throw new RuntimeException("Post nao encontrado");
        }
        return postRepository.findByClient(client);
    }

    public List<PostEntity> findByUser(UserEntity user){
        if(postRepository.findByUser(user).isEmpty()){
            throw new RuntimeException("Post nao encontrado");
        }
        return postRepository.findByUser(user);
    }

    public List<PostEntity> findByScheduledAt(LocalDateTime start, LocalDateTime end){
        if(postRepository.findByScheduledAtBetween(start, end).isEmpty()){
            throw new RuntimeException("Post nao encontrado");
        }
        return postRepository.findByScheduledAtBetween(start, end);
    }

    public PostEntity savePost(PostEntity post){
        return postRepository.save(post);
    }

    public PostEntity updatePost(PostEntity post){
        if(postRepository.findById(post.getId()).isEmpty()){
            throw new RuntimeException("Post nao encontrado");
        }
        return postRepository.save(post);
    }
    public void deletePostById(Long id){
        if(postRepository.findById(id).isEmpty()){
            throw new RuntimeException("Post nao encontrado");
        }
        postRepository.deleteById(id);
    }

}
