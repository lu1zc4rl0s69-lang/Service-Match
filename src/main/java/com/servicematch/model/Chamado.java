package com.servicematch.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_CHAMADO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Chamado {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_chamado")
    @SequenceGenerator(name = "seq_chamado", sequenceName = "SEQ_CHAMADO", allocationSize = 1)
    @Column(name = "ID_CHAMADO")
    private Long id;

    @Column(name = "CD_CHAMADO", unique = true, length = 20)
    private String codigo;

    @NotBlank
    @Size(max = 150)
    @Column(name = "DS_TITULO", nullable = false, length = 150)
    private String titulo;

    @Column(name = "DS_DESCRICAO", length = 2000)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CLIENTE", nullable = false)
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PROFISSIONAL")
    private Profissional profissional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CATEGORIA")
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "ST_CHAMADO", nullable = false, length = 20)
    private StatusChamado status = StatusChamado.ABERTO;

    @Enumerated(EnumType.STRING)
    @Column(name = "TP_PRIORIDADE", length = 10)
    private PrioridadeChamado prioridade = PrioridadeChamado.MEDIA;

    @Column(name = "DS_ENDERECO", length = 200)
    private String endereco;

    @Column(name = "DS_CIDADE", length = 100)
    private String cidade;

    @Column(name = "NR_CUSTO_TOKENS")
    private Integer custoTokens = 10;

    @Column(name = "DT_ABERTURA", nullable = false, updatable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "DT_CONCLUSAO")
    private LocalDateTime dataConclusao;

    @PrePersist
    protected void onCreate() {
        dataAbertura = LocalDateTime.now();
        if (codigo == null) {
            codigo = "CH-" + System.currentTimeMillis();
        }
    }
}
