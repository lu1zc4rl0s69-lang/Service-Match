package com.servicematch.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_TOKEN_MOVIMENTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenMovimento {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_token")
    @SequenceGenerator(name = "seq_token", sequenceName = "SEQ_TOKEN", allocationSize = 1)
    @Column(name = "ID_TOKEN")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PROFISSIONAL", nullable = false)
    private Profissional profissional;

    @Column(name = "TP_MOVIMENTO", nullable = false, length = 20)
    private String tipo; // COMPRA, DESBLOQUEIO, BONUS

    @Column(name = "QT_TOKENS", nullable = false)
    private Integer quantidade;

    @Column(name = "VL_PAGO", precision = 10, scale = 2)
    private BigDecimal valorPago;

    @Column(name = "DS_DESCRICAO", length = 200)
    private String descricao;

    @Column(name = "DT_MOVIMENTO", nullable = false, updatable = false)
    private LocalDateTime dataMovimento;

    @PrePersist
    protected void onCreate() {
        dataMovimento = LocalDateTime.now();
    }
}
