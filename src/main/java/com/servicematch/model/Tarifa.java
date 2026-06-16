package com.servicematch.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "TB_TARIFA")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_tarifa")
    @SequenceGenerator(name = "seq_tarifa", sequenceName = "SEQ_TARIFA", allocationSize = 1)
    @Column(name = "ID_TARIFA")
    private Long id;

    @NotBlank
    @Column(name = "NM_TARIFA", nullable = false, length = 100)
    private String nome;

    @Column(name = "TP_TARIFA", nullable = false, length = 20)
    private String tipo; // PERCENTUAL ou FIXO

    @Column(name = "VL_TARIFA", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "DS_APLICACAO", length = 100)
    private String aplicacao;

    @Column(name = "DS_DESCRICAO", length = 500)
    private String descricao;

    @Column(name = "FL_ATIVA", nullable = false)
    private boolean ativa = true;
}
