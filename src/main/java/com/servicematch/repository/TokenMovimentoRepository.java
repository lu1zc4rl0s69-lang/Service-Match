package com.servicematch.repository;

import com.servicematch.model.TokenMovimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TokenMovimentoRepository extends JpaRepository<TokenMovimento, Long> {
    List<TokenMovimento> findByProfissionalIdOrderByDataMovimentoDesc(Long profissionalId);
}
