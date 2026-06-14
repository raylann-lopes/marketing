package com.north.producoes.entity;

import com.north.producoes.entity.enums.ClientStatusEnum;
import com.north.producoes.integration.apify.dto.ContentIdeaTermsDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tb_client")
public class ClientEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String number;

    @Column
    private String driveLink;

    @Column(nullable = false)
    private String niche;

    @Column(nullable = false, length = 1024)
    private String voiceTone;

    @Column(name = "whatsapp_group_id", unique = true, length = 100)
    private String whatsappGroupId;

    @Column(name = "whatsapp_group_name")
    private String whatsappGroupName;

    @Column(name = "monthly_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyValue = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ClientStatusEnum status = ClientStatusEnum.ACTIVE;

    @Column
    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "client")
    private List<PostEntity> posts;

    @OneToMany(mappedBy = "client")
    private List<ContentIdeaEntity> contentIdeas;

    @OneToMany(mappedBy = "client")
    private List<ContentIdeaRunsEntity> contentIdeaRuns;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_terms", columnDefinition = "jsonb")
    private ContentIdeaTermsDTO aiTerms;
}
