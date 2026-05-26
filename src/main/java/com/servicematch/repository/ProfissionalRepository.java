package com.servicematch.repository;

import com.servicematch.model.Profissional;
import com.servicematch.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    Optional<Profissional> findByUsuarioId(Long usuarioId);

    List<Profissional> findByDisponivelTrue();

    List<Profissional> findByCategoria(Categoria categoria);

    List<Profissional> findByCidadeIgnoreCase(String cidade);

    @Query("SELECT p FROM Profissional p WHERE " +
           "(:categoriaId IS NULL OR p.categoria.id = :categoriaId) AND " +
           "(:cidade IS NULL OR LOWER(p.cidade) LIKE LOWER(CONCAT('%', :cidade, '%'))) AND " +
           "(:disponivel IS NULL OR p.disponivel = :disponivel) AND " +
           "p.usuario.ativo = true")
    List<Profissional> buscarComFiltros(
            @Param("categoriaId") Long categoriaId,
            @Param("cidade") String cidade,
            @Param("disponivel") Boolean disponivel);

    @Query("SELECT p FROM Profissional p WHERE " +
           "LOWER(p.usuario.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(p.descricao) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(p.categoria.nome) LIKE LOWER(CONCAT('%', :termo, '%'))")
    List<Profissional> buscarPorTermo(@Param("termo") String termo);
}
