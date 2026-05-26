package com.servicematch.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_USUARIO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_usuario")
    @SequenceGenerator(name = "seq_usuario", sequenceName = "SEQ_USUARIO", allocationSize = 1)
    @Column(name = "ID_USUARIO")
    private Long id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(name = "NM_USUARIO", nullable = false, length = 100)
    private String nome;

    @NotBlank
    @Email
    @Column(name = "DS_EMAIL", nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank
    @Column(name = "DS_SENHA", nullable = false)
    private String senha;

    @Size(max = 20)
    @Column(name = "NR_TELEFONE", length = 20)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(name = "TP_USUARIO", nullable = false, length = 20)
    private TipoUsuario tipo;

    @Column(name = "FL_ATIVO", nullable = false)
    private boolean ativo = true;

    @Column(name = "DT_CADASTRO", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "DT_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @PrePersist
    protected void onCreate() {
        dataCadastro = LocalDateTime.now();
        dataAtualizacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }
}
