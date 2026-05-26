package com.servicematch.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "TB_CATEGORIA")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_categoria")
    @SequenceGenerator(name = "seq_categoria", sequenceName = "SEQ_CATEGORIA", allocationSize = 1)
    @Column(name = "ID_CATEGORIA")
    private Long id;

    @NotBlank
    @Column(name = "NM_CATEGORIA", nullable = false, unique = true, length = 80)
    private String nome;

    @Column(name = "DS_ICONE", length = 10)
    private String icone;

    @Column(name = "FL_ATIVA", nullable = false)
    private boolean ativa = true;
}
