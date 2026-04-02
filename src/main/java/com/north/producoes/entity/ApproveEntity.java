package com.north.producoes.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TimeZoneStorage;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tb_post_approvals")
public class ApproveEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JsonIgnore
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    @Column(name = "art_url", nullable = false)
    private String artUrl;

    @Column(name = "art_name", nullable = false)
    private String artName;

    @Column(name = "caption", nullable = false)
    private String caption;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ApproveStatusEnum status = ApproveStatusEnum.PENDING;

    @Column
    @TimeZoneStorage
    private LocalDateTime approvedAt;

    @Column(name = "approved_user", nullable = false)
    private String ApprovedUser;
}
