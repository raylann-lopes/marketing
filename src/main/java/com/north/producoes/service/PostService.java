package com.north.producoes.service;

import com.north.producoes.controller.dto.request.PostRequestDTO;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ApproveRepository;
import com.north.producoes.repository.ClientRepository;
import com.north.producoes.repository.PostRepository;
import com.north.producoes.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final OpenAiService openAiService;
    private final ApproveRepository approveRepository;
    private final S3Service s3Service;

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

    public List<PostEntity> findByUser(Long userId){
        List<PostEntity> postUser = postRepository.findByUserId(userId);
        if(postUser.isEmpty()){
            throw new ResourceNotFoundException("Nenhum post encontrado para o usuario informado");
        }
        return postUser.stream().toList();
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
        if (dto.clientId() == null || dto.userId() == null) {
            throw new IllegalArgumentException("Cliente e Usuário são obrigatórios para novas demandas");
        }
        ClientEntity client = clientRepository.findById(dto.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + dto.clientId()));
        UserEntity user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + dto.userId()));

        PostEntity post = new PostEntity();
        return getPostEntity(dto, post, client, user);
    }

    @Transactional
    public PostEntity updatePost(Long id, PostRequestDTO dto){
        PostEntity postExisting = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post não encontrado com id: " + id));

        ClientEntity client = null;
        if (dto.clientId() != null) {
            client = clientRepository.findById(dto.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + dto.clientId()));
        }

        UserEntity user = null;
        if (dto.userId() != null) {
            user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + dto.userId()));
        }

        return getPostEntity(dto, postExisting, client, user);
    }

    @NonNull
    private PostEntity getPostEntity(PostRequestDTO dto, PostEntity postExisting, ClientEntity client, UserEntity user) {
        postExisting.setTitle(dto.title());
        postExisting.setTheme(dto.theme());
        postExisting.setObjective(dto.objective());
        postExisting.setStatus(dto.status());
        postExisting.setIsUrgent(dto.isUrgent() != null ? dto.isUrgent() : false);
        postExisting.setScheduledAt(dto.scheduledAt());
        
        if (dto.format() != null) {
            postExisting.setFormat(dto.format());
        }

        if (postExisting.getCarouselImages() != null) {
            postExisting.getCarouselImages().clear();
        } else {
            postExisting.setCarouselImages(new java.util.ArrayList<>());
        }

        List<String> refs = dto.referenceImageS3Keys();
        if (refs != null && !refs.isEmpty()) {
            int order = 0;
            for (String key : refs) {
                com.north.producoes.entity.PostCarouselImageEntity refEntity = new com.north.producoes.entity.PostCarouselImageEntity();
                refEntity.setPost(postExisting);
                refEntity.setS3Key(key);
                refEntity.setSortOrder(order++);
                postExisting.getCarouselImages().add(refEntity);
            }
            postExisting.setReferenceImageS3Key(refs.get(0));
        } else if (StringUtils.hasText(dto.referenceImageS3Key())) {
            postExisting.setReferenceImageS3Key(dto.referenceImageS3Key());
        }
        
        if (client != null) {
            postExisting.setClient(client);
        }
        if (user != null) {
            postExisting.setUser(user);
        }

        return postRepository.save(postExisting);
    }

    @Transactional
    public PostEntity updateReferenceImage(Long id, List<String> s3Keys) {
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post não encontrado com id: " + id));
        
        if (post.getCarouselImages() != null) {
            post.getCarouselImages().clear();
        } else {
            post.setCarouselImages(new java.util.ArrayList<>());
        }

        if (s3Keys != null && !s3Keys.isEmpty()) {
            int order = 0;
            for (String key : s3Keys) {
                com.north.producoes.entity.PostCarouselImageEntity refEntity = new com.north.producoes.entity.PostCarouselImageEntity();
                refEntity.setPost(post);
                refEntity.setS3Key(key);
                refEntity.setSortOrder(order++);
                post.getCarouselImages().add(refEntity);
            }
            post.setReferenceImageS3Key(s3Keys.get(0));
        }

        return postRepository.save(post);
    }

    @Transactional
    public void deletePostById(Long id){
        if(!postRepository.existsById(id)){
            throw new ResourceNotFoundException("Post nao encontrado com id: " + id);
        }
        approveRepository.deleteByPostId(id);
        postRepository.deleteById(id);
    }

    @Transactional
    public ApproveEntity generateCaption(Long id, String manualArtS3Key){
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post não encontrado com id: " + id));

        String cleanS3Key = sanitizeS3Key(manualArtS3Key);
        ApproveEntity approve = approveRepository.findByPostId(id).stream().findFirst().orElse(null);
        String finalS3Key = resolveArtS3Key(cleanS3Key, approve);
        String imageUrl = resolveImageUrl(finalS3Key);

        String caption = openAiService.generateCaption(post, imageUrl);

        if (approve != null) {
            approve.setCaption(caption);
            return approveRepository.save(approve);
        }

        ApproveEntity tempApprove = new ApproveEntity();
        tempApprove.setPost(post);
        tempApprove.setCaption(caption);
        tempApprove.setArtName("Legenda Gerada");
        tempApprove.setStatus(ApproveStatusEnum.PENDING);
        tempApprove.setArtS3Key(finalS3Key);
        return tempApprove;
    }

    private String sanitizeS3Key(String artS3Key) {
        if (!StringUtils.hasText(artS3Key)) return null;
        String cleaned = artS3Key.trim().replace("\"", "").replace("{", "").replace("}", "");
        if (cleaned.startsWith("artS3Key:")) cleaned = cleaned.substring("artS3Key:".length()).trim();
        return StringUtils.hasText(cleaned) ? cleaned : null;
    }

    private String resolveArtS3Key(String manualArtS3Key, ApproveEntity approve) {
        if (StringUtils.hasText(manualArtS3Key)) return manualArtS3Key;
        if (approve != null && StringUtils.hasText(approve.getArtS3Key())) return approve.getArtS3Key();
        return null;
    }

    private String resolveImageUrl(String s3Key) {
        if (!StringUtils.hasText(s3Key)) return null;
        return s3Service.resolveReadUrl(s3Key);
    }
}
