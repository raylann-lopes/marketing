package com.north.producoes.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.north.producoes.entity.enums.PostStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tb_posts")
public class PostEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String theme;

    @Column(nullable = false)
    private String objective;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PostStatusEnum status = PostStatusEnum.DEMAND;

    @Column(name = "is_urgent")
    private Boolean isUrgent = false;

    @Column(name = "reference_image_s3_key")
    private String referenceImageS3Key;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @OneToOne(mappedBy = "post")
    @JsonIgnore
    private ApproveEntity approve;
}
