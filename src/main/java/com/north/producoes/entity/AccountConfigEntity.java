package com.north.producoes.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tb_account_config")
public class AccountConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "client_id", nullable = false, unique = true)
    private ClientEntity client;

    @Column(name = "ig_user_id", nullable = false, length = 64)
    private String igUserId;

    @Column(name = "access_token", length = 2048)
    private String accessToken;

    // Email do admin que realizou a configuração — auditoria
    @Column(name = "configured_by", nullable = false)
    private String configuredBy;

    @Column(name = "configured_at", nullable = false)
    private LocalDateTime configuredAt;
}
