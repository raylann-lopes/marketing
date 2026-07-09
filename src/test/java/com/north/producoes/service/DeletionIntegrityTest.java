package com.north.producoes.service;

import com.north.producoes.entity.ApproveCarouselArtEntity;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostCarouselImageEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.PostFormatEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.entity.enums.UserRoleEnum;
import com.north.producoes.repository.ApproveRepository;
import com.north.producoes.repository.ClientRepository;
import com.north.producoes.repository.PostRepository;
import com.north.producoes.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova que os fluxos de deleção não violam integridade referencial.
 *
 * Roda contra H2 com schema gerado pelo Hibernate — SEM ON DELETE CASCADE
 * no banco, o pior cenário. Se os services não removerem os filhos na ordem
 * certa (bulk delete ignora o cascade do JPA), estes testes falham com
 * violação de FK.
 */
@SpringBootTest
@Transactional
@DisplayName("Integridade referencial nos fluxos de deleção")
class DeletionIntegrityTest {

    @Autowired private ClientService clientService;
    @Autowired private PostService postService;
    @Autowired private ApprovedService approvedService;
    @Autowired private ClientRepository clientRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private ApproveRepository approveRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private ClientEntity client;
    private PostEntity post;
    private ApproveEntity approve;

    @BeforeEach
    void seedFullGraph() {
        UserEntity user = new UserEntity();
        user.setName("Tester");
        user.setEmail("tester-" + System.nanoTime() + "@example.com");
        user.setPassword("secret");
        user.setRole(UserRoleEnum.ADMIN);
        user = userRepository.save(user);

        client = new ClientEntity();
        client.setName("Cliente Integridade");
        client.setNumber("55" + System.nanoTime());
        client.setNiche("Saude");
        client.setVoiceTone("Formal");
        client = clientRepository.save(client);

        post = new PostEntity();
        post.setTitle("Post carrossel");
        post.setTheme("Tema");
        post.setObjective("Objetivo");
        post.setStatus(PostStatusEnum.FINISHED);
        post.setScheduledAt(LocalDateTime.now().plusDays(1));
        post.setClient(client);
        post.setUser(user);
        post.setFormat(PostFormatEnum.CAROUSEL);
        post.setReferenceImageS3Key("public/posts/references/1/1/ref0.png");
        for (int i = 0; i < 2; i++) {
            PostCarouselImageEntity ref = new PostCarouselImageEntity();
            ref.setPost(post);
            ref.setS3Key("public/posts/references/1/1/ref" + i + ".png");
            ref.setSortOrder(i);
            post.getCarouselImages().add(ref);
        }
        post = postRepository.save(post);

        approve = new ApproveEntity();
        approve.setPost(post);
        approve.setArtS3Key("public/posts/1/1/art0.png");
        approve.setArtName("art0.png");
        approve.setCaption("Legenda");
        approve.setApprovedUser("");
        for (int i = 0; i < 3; i++) {
            ApproveCarouselArtEntity art = new ApproveCarouselArtEntity();
            art.setApprove(approve);
            art.setS3Key("public/posts/1/1/art" + i + ".png");
            art.setArtName("art" + i + ".png");
            art.setSortOrder(i);
            approve.getCarouselArts().add(art);
        }
        approve = approveRepository.save(approve);
        approveRepository.flush();
    }

    @Test
    @DisplayName("deletar cliente remove posts, aprovações e filhos de carrossel sem violar FK")
    void deleteClientRemovesWholeGraph() {
        clientService.deleteClientById(client.getId());
        entityManager.flush();

        assertThat(clientRepository.existsById(client.getId())).isFalse();
        assertThat(postRepository.existsById(post.getId())).isFalse();
        assertThat(approveRepository.existsById(approve.getId())).isFalse();
        assertThat(countCarouselArts()).isZero();
        assertThat(countCarouselImages()).isZero();
    }

    @Test
    @DisplayName("deletar post remove aprovação e filhos de carrossel sem violar FK")
    void deletePostRemovesApprovalGraph() {
        postService.deletePostById(post.getId());
        entityManager.flush();

        assertThat(postRepository.existsById(post.getId())).isFalse();
        assertThat(approveRepository.existsById(approve.getId())).isFalse();
        assertThat(countCarouselArts()).isZero();
        assertThat(countCarouselImages()).isZero();
        // Cliente permanece intacto
        assertThat(clientRepository.existsById(client.getId())).isTrue();
    }

    @Test
    @DisplayName("deletar aprovação remove as artes do carrossel e preserva o post")
    void deleteApprovalRemovesArts() {
        approvedService.deleteApproveById(approve.getId());
        entityManager.flush();

        assertThat(approveRepository.existsById(approve.getId())).isFalse();
        assertThat(countCarouselArts()).isZero();
        assertThat(postRepository.existsById(post.getId())).isTrue();
    }

    private long countCarouselArts() {
        return entityManager.createQuery(
                "SELECT COUNT(a) FROM ApproveCarouselArtEntity a", Long.class).getSingleResult();
    }

    private long countCarouselImages() {
        return entityManager.createQuery(
                "SELECT COUNT(i) FROM PostCarouselImageEntity i", Long.class).getSingleResult();
    }
}
