package com.servicematch.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.servicematch.model.Avaliacao;

import java.util.List;
import java.util.Optional;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    List<Avaliacao> findByProfissionalIdOrderByDataAvaliacaoDesc(Long profissionalId);
    List<Avaliacao> findByClienteIdOrderByDataAvaliacaoDesc(Long clienteId);
    Optional<Avaliacao> findByChamadoId(Long chamadoId);
    boolean existsByChamadoId(Long chamadoId);

    @Query("SELECT AVG(a.nota) FROM Avaliacao a WHERE a.profissional.id = :profissionalId")
    Double calcularMediaPorProfissional(@Param("profissionalId") Long profissionalId);

    long countByProfissionalId(Long profissionalId);
}

