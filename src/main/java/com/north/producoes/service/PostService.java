package com.north.producoes.service;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    public List<PostEntity> findAllPost(){
        return postRepository.findAll();
    }

    public List<PostEntity> findByStatus(PostStatusEnum status){
        List<PostEntity> postStatus = postRepository.findByStatus(status);
        if(postStatus.isEmpty()){
            throw new ResourceNotFoundException("Nenhum post encontrado com status: " + status);
        }
        return postStatus;
    }

    public List<PostEntity> findByClient(ClientEntity client){
        List<PostEntity> postClient = postRepository.findByClient(client);
        if(postClient.isEmpty()){
            throw new ResourceNotFoundException("Nenhum post encontrado para o cliente informado");
        }
        return postClient;
    }

    public List<PostEntity> findByUser(UserEntity user){
        List<PostEntity> postUser = postRepository.findByUser(user);
        if(postUser.isEmpty()){
            throw new ResourceNotFoundException("Nenhum post encontrado para o usuario informado");
        }
        return postUser;
    }

    public List<PostEntity> findByScheduledAt(LocalDateTime scheduledAtAfter, LocalDateTime scheduledAtBefore){
        if(scheduledAtAfter.isAfter(scheduledAtBefore)){
            throw new IllegalArgumentException("Data de inicio deve ser anterior a data de fim");
        }
        List<PostEntity> postScheduledAt = postRepository.findByScheduledAtBetween(scheduledAtAfter, scheduledAtBefore);
        if(postScheduledAt.isEmpty()){
            throw new ResourceNotFoundException("Nenhum post encontrado no periodo informado");
        }
        return postScheduledAt;
    }

    @Transactional
    public PostEntity savePost(PostEntity post){
        return postRepository.save(post);
    }

    @Transactional
    public PostEntity updatePost(PostEntity post){
            PostEntity postExisting = postRepository.findById(post.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Post nao encontrado com id: " + post.getId()));
            postExisting.setClient(post.getClient());
            postExisting.setScheduledAt(post.getScheduledAt());
            postExisting.setStatus(post.getStatus());
            postExisting.setTitle(post.getTitle());
            postExisting.setUser(post.getUser());
            return postExisting;
        }

    @Transactional
    public void deletePostById(Long id){
        if(!postRepository.existsById(id)){
            throw new ResourceNotFoundException("Post nao encontrado com id: " + id);
        }
        postRepository.deleteById(id);
    }
}
