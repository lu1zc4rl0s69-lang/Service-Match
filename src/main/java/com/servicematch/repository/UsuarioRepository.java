package com.servicematch.repository;

import com.servicematch.model.Usuario;
import com.servicematch.model.TipoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Usuario> findByTipo(TipoUsuario tipo);
    List<Usuario> findByAtivoTrue();
}
