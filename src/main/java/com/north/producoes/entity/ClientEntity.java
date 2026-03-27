package com.north.producoes.entity;

import com.north.producoes.entity.enums.ClientStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

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

    @Column(nullable = false)
    private String driveLink;

    @Column(nullable = false)
    private String niche;

    @Column
    private String voiceTone;

    @Column
    @Enumerated(EnumType.STRING)
    private ClientStatusEnum status = ClientStatusEnum.ACTIVE;

    @Column
    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "client_id")
    private List<PostEntity> post;
}
