package com.north.producoes.entity;

import com.north.producoes.entity.enums.FinanceStatusEnum;
import com.north.producoes.entity.enums.FinanceTypeEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_finance")
public class FinanceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal value;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FinanceStatusEnum status = FinanceStatusEnum.PENDING;

    @Column
    @Enumerated(EnumType.STRING)
    private FinanceTypeEnum type;

    @Column(nullable = false)
    private LocalDateTime expirationDate;

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    @Column
    @CreationTimestamp
    private LocalDateTime createdAt;
}
