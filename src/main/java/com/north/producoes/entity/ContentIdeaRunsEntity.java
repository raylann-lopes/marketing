package com.north.producoes.entity;

import com.north.producoes.entity.enums.ContentIdeaRunsStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tb_content_idea_runs", indexes = {
        @Index(name = "idx_content_idea_runs_client_id", columnList = "client_id"),
        @Index(name = "idx_content_idea_runs_status", columnList = "status"),
})

public class ContentIdeaRunsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private ClientEntity client;

    private String niche;

    private String provider = "apify";

    @Enumerated(EnumType.STRING)
    private ContentIdeaRunsStatusEnum status;

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "ideas_count")
    private Integer ideasCount;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;
}