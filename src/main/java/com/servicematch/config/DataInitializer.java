package com.servicematch.config;

import com.servicematch.model.*;
import com.servicematch.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UsuarioRepository usuarioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final CategoriaRepository categoriaRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            if (usuarioRepository.count() > 0) {
                log.info("Banco já populado. Pulando inicialização.");
                return;
            }

            log.info("Inicializando dados de exemplo...");

            // Categorias
            Categoria eletricista = salvarCategoria("Eletricista", "⚡");
            Categoria encanador   = salvarCategoria("Encanador", "🔧");
            Categoria pintor      = salvarCategoria("Pintor", "🎨");
            Categoria pedreiro    = salvarCategoria("Pedreiro", "🧱");
            Categoria jardineiro  = salvarCategoria("Jardineiro", "🌱");
            Categoria mecanico    = salvarCategoria("Mecânico", "🚗");
            Categoria tecnico_ti  = salvarCategoria("Técnico de TI", "💻");
            Categoria marceneiro  = salvarCategoria("Marceneiro", "🪚");

            // Admin
            Usuario admin = new Usuario();
            admin.setNome("Administrador");
            admin.setEmail("admin@servicematch.com");
            admin.setSenha(passwordEncoder.encode("admin123"));
            admin.setTelefone("(11) 99999-0000");
            admin.setTipo(TipoUsuario.ADMIN);
            usuarioRepository.save(admin);

            // Cliente de exemplo
            Usuario cliente1 = new Usuario();
            cliente1.setNome("Ana Costa");
            cliente1.setEmail("ana@email.com");
            cliente1.setSenha(passwordEncoder.encode("123456"));
            cliente1.setTelefone("(11) 98888-1111");
            cliente1.setTipo(TipoUsuario.CLIENTE);
            usuarioRepository.save(cliente1);

            // Profissional 1
            Usuario userProf1 = new Usuario();
            userProf1.setNome("Carlos Silva");
            userProf1.setEmail("carlos@email.com");
            userProf1.setSenha(passwordEncoder.encode("123456"));
            userProf1.setTelefone("(11) 97777-2222");
            userProf1.setTipo(TipoUsuario.PROFISSIONAL);
            usuarioRepository.save(userProf1);

            Profissional prof1 = new Profissional();
            prof1.setUsuario(userProf1);
            prof1.setCategoria(eletricista);
            prof1.setCidade("São Paulo");
            prof1.setEstado("SP");
            prof1.setPrecoBase(new BigDecimal("80.00"));
            prof1.setDescricao("Eletricista especializado em instalações residenciais e comerciais. +15 anos de experiência.");
            prof1.setDisponivel(true);
            prof1.setVerificado(true);
            prof1.setAvaliacaoMedia(new BigDecimal("4.9"));
            prof1.setQuantidadeAvaliacoes(127);
            prof1.setTokens(45);
            profissionalRepository.save(prof1);

            // Profissional 2
            Usuario userProf2 = new Usuario();
            userProf2.setNome("Maria Santos");
            userProf2.setEmail("maria@email.com");
            userProf2.setSenha(passwordEncoder.encode("123456"));
            userProf2.setTelefone("(11) 96666-3333");
            userProf2.setTipo(TipoUsuario.PROFISSIONAL);
            usuarioRepository.save(userProf2);

            Profissional prof2 = new Profissional();
            prof2.setUsuario(userProf2);
            prof2.setCategoria(encanador);
            prof2.setCidade("São Paulo");
            prof2.setEstado("SP");
            prof2.setPrecoBase(new BigDecimal("70.00"));
            prof2.setDescricao("Encanadora profissional. Reparos, instalações e manutenção hidráulica.");
            prof2.setDisponivel(true);
            prof2.setVerificado(true);
            prof2.setAvaliacaoMedia(new BigDecimal("4.7"));
            prof2.setQuantidadeAvaliacoes(89);
            prof2.setTokens(20);
            profissionalRepository.save(prof2);

            log.info("✅ Dados inicializados com sucesso!");
            log.info("👤 Admin:        admin@servicematch.com / admin123");
            log.info("👤 Cliente:      ana@email.com / 123456");
            log.info("👤 Profissional: carlos@email.com / 123456");
        };
    }

    private Categoria salvarCategoria(String nome, String icone) {
        Categoria c = new Categoria();
        c.setNome(nome);
        c.setIcone(icone);
        return categoriaRepository.save(c);
    }
}
