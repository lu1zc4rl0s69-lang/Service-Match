package com.servicematch.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "TB_PROFISSIONAL")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Profissional {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_profissional")
    @SequenceGenerator(name = "seq_profissional", sequenceName = "SEQ_PROFISSIONAL", allocationSize = 1)
    @Column(name = "ID_PROFISSIONAL")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO", nullable = false, unique = true)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CATEGORIA")
    private Categoria categoria;

    @Column(name = "DS_CIDADE", length = 100)
    private String cidade;

    @Column(name = "DS_ESTADO", length = 2)
    private String estado;

    @Column(name = "VL_PRECO_BASE", precision = 10, scale = 2)
    private BigDecimal precoBase;

    @Column(name = "DS_DESCRICAO", length = 1000)
    private String descricao;

    @Column(name = "FL_DISPONIVEL", nullable = false)
    private boolean disponivel = true;

    @Column(name = "FL_VERIFICADO", nullable = false)
    private boolean verificado = false;

    @Column(name = "NR_AVALIACAO_MEDIA", precision = 3, scale = 2)
    private BigDecimal avaliacaoMedia = BigDecimal.ZERO;

    @Column(name = "QT_AVALIACOES")
    private Integer quantidadeAvaliacoes = 0;

    @Column(name = "QT_TOKENS")
    private Integer tokens = 0;

    @Column(name = "DS_FOTO_URL", length = 255)
    private String fotoUrl;

    @Column(name = "DS_SERVICOS", length = 1000)
    private String servicos;

    @Column(name = "DS_PORTFOLIO_FOTOS", length = 2000)
    private String portfolioFotos;
}
