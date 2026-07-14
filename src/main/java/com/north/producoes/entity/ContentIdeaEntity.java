package com.north.producoes.entity;

import com.north.producoes.entity.enums.ContentIdeaPriorityEnum;
import com.north.producoes.entity.enums.ContentIdeaStatusEnum;
import com.north.producoes.entity.enums.ContentIdeiaFormatEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tb_content_ideas", indexes = {
        @Index(name = "idx_content_idea_client_id", columnList = "client_id"),
        @Index(name = "idx_content_idea_status", columnList = "status"),
        @Index(name = "idx_content_idea_generated_at", columnList = "created_at")
})
public class ContentIdeaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private ClientEntity client;

    private String title;

    private String theme;

    private String objective;

    @Enumerated(EnumType.STRING)
    private ContentIdeiaFormatEnum format;

    private String hook;

    @Column(length = 1024)
    private String reason;

    @Column(name = "source_terms", length = 1024)
    private String sourceTerms;

    @Column(name = "signal_summary", length = 1024)
    private String signalSummary;

    // Score do sinal viral que originou a ideia (likes + 2*comments)
    @Column(name = "engagement_score")
    private Double engagementScore;

    @Enumerated(EnumType.STRING)
    private ContentIdeaStatusEnum status = ContentIdeaStatusEnum.SUGGESTED;

    @Enumerated(EnumType.STRING)
    private ContentIdeaPriorityEnum priority = ContentIdeaPriorityEnum.MEDIUM;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_post_id")
    private PostEntity convertedPost;

}
