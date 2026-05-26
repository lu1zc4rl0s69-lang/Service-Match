package com.servicematch.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_AVALIACAO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_avaliacao")
    @SequenceGenerator(name = "seq_avaliacao", sequenceName = "SEQ_AVALIACAO", allocationSize = 1)
    @Column(name = "ID_AVALIACAO")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CHAMADO", nullable = false, unique = true)
    private Chamado chamado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PROFISSIONAL", nullable = false)
    private Profissional profissional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CLIENTE", nullable = false)
    private Usuario cliente;

    @Min(1) @Max(5)
    @Column(name = "NR_NOTA", nullable = false)
    private Integer nota;

    @Column(name = "DS_COMENTARIO", length = 500)
    private String comentario;

    @Column(name = "DT_AVALIACAO", nullable = false, updatable = false)
    private LocalDateTime dataAvaliacao;

    @PrePersist
    protected void onCreate() {
        dataAvaliacao = LocalDateTime.now();
    }
}
