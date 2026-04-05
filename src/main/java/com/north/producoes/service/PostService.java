package com.north.producoes.service;

import com.north.producoes.controller.dto.request.PostRequestDTO;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ClientRepository;
import com.north.producoes.repository.PostRepository;
import com.north.producoes.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

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

    public List<PostEntity> findByClient(Long clientId){
        List<PostEntity> postClient = postRepository.findByClientId(clientId);
        if(postClient.isEmpty()){
            throw new ResourceNotFoundException("Nenhum post encontrado para o cliente informado");
        }
        return postClient;
    }

    public PostEntity findByUser(Long userId){
        List<PostEntity> postUser = postRepository.findByUserId(userId);
        if(postUser.isEmpty()){
            throw new ResourceNotFoundException("Nenhum post encontrado para o usuario informado");
        }
        return postUser.getFirst();
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
    public PostEntity savePost(PostRequestDTO dto){
        ClientEntity client = clientRepository.findById(dto.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + dto.clientId()));
        UserEntity user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + dto.userId()));

        PostEntity post = new PostEntity();
        post.setTitle(dto.title());
        post.setTheme(dto.theme());
        post.setObjective(dto.objective());
        post.setStatus(dto.status());
        post.setScheduledAt(dto.scheduledAt());
        post.setClient(client);
        post.setUser(user);

        return postRepository.save(post);
    }

    @Transactional
    public PostEntity updatePost(Long id, PostRequestDTO dto){
        PostEntity postExisting = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post não encontrado com id: " + id));

        ClientEntity client = clientRepository.findById(dto.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + dto.clientId()));
        UserEntity user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + dto.userId()));

        postExisting.setTitle(dto.title());
        postExisting.setTheme(dto.theme());
        postExisting.setObjective(dto.objective());
        postExisting.setStatus(dto.status());
        postExisting.setScheduledAt(dto.scheduledAt());
        postExisting.setClient(client);
        postExisting.setUser(user);

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
