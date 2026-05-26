package com.servicematch.repository;

import com.servicematch.model.Chamado;
import com.servicematch.model.StatusChamado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChamadoRepository extends JpaRepository<Chamado, Long> {
    List<Chamado> findByClienteIdOrderByDataAberturaDesc(Long clienteId);
    List<Chamado> findByProfissionalIdOrderByDataAberturaDesc(Long profissionalId);
    List<Chamado> findByStatus(StatusChamado status);
    List<Chamado> findByStatusOrderByDataAberturaDesc(StatusChamado status);
    List<Chamado> findByCategoriaIdAndStatus(Long categoriaId, StatusChamado status);
    long countByStatus(StatusChamado status);
    long countByClienteId(Long clienteId);
    long countByProfissionalId(Long profissionalId);
}
