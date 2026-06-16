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
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UsuarioRepository usuarioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final CategoriaRepository categoriaRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Map<String, String[]> CATEGORIAS_DATA = new LinkedHashMap<>();
    static {
        CATEGORIAS_DATA.put("Eletricista", new String[]{
            "bi-lightning-charge-fill",
            "Instalações elétricas, reparos e manutenção elétrica residencial e comercial."
        });
        CATEGORIAS_DATA.put("Encanador", new String[]{
            "bi-droplet-fill",
            "Serviços hidráulicos, reparos em tubulações e instalações de água."
        });
        CATEGORIAS_DATA.put("Pintor", new String[]{
            "bi-brush-fill",
            "Pintura residencial e comercial, texturas, grafiato e acabamentos finos."
        });
        CATEGORIAS_DATA.put("Pedreiro", new String[]{
            "bi-bricks",
            "Construção, reformas, alvenaria e serviços gerais de obra civil."
        });
        CATEGORIAS_DATA.put("Jardineiro", new String[]{
            "bi-flower1",
            "Jardinagem, paisagismo, poda e manutenção de áreas verdes."
        });
        CATEGORIAS_DATA.put("Mecânico", new String[]{
            "bi-car-front-fill",
            "Manutenção preventiva e corretiva de veículos automotores."
        });
        CATEGORIAS_DATA.put("Técnico de TI", new String[]{
            "bi-cpu-fill",
            "Suporte técnico, configuração de redes e manutenção de computadores."
        });
        CATEGORIAS_DATA.put("Marceneiro", new String[]{
            "bi-hammer",
            "Móveis sob medida, reparos em madeira e marcenaria em geral."
        });
    }

    @Bean
    public CommandLineRunner initData() {
        return args -> {

            // Cria categorias ausentes e atualiza ícones/descrições das existentes
            for (Map.Entry<String, String[]> entry : CATEGORIAS_DATA.entrySet()) {
                String nome = entry.getKey();
                String[] dados = entry.getValue();
                categoriaRepository.findAll().stream()
                    .filter(c -> c.getNome().equals(nome))
                    .findFirst()
                    .ifPresentOrElse(
                        cat -> {
                            cat.setIcone(dados[0]);
                            cat.setDescricao(dados[1]);
                            categoriaRepository.save(cat);
                        },
                        () -> {
                            Categoria nova = new Categoria();
                            nova.setNome(nome);
                            nova.setIcone(dados[0]);
                            nova.setDescricao(dados[1]);
                            categoriaRepository.save(nova);
                        }
                    );
            }

            if (usuarioRepository.count() > 0) {
                log.info("Usuários já existem. Pulando seed inicial.");
                return;
            }

            log.info("Criando usuários iniciais...");

            // Admin
            Usuario admin = new Usuario();
            admin.setNome("Administrador");
            admin.setEmail("admin@servicematch.com");
            admin.setSenha(passwordEncoder.encode("admin123"));
            admin.setTelefone("(83) 99999-0000");
            admin.setTipo(TipoUsuario.ADMIN);
            admin.setAtivo(true);
            usuarioRepository.save(admin);

            // Cliente
            Usuario ana = new Usuario();
            ana.setNome("Ana Costa");
            ana.setEmail("ana@email.com");
            ana.setSenha(passwordEncoder.encode("123456"));
            ana.setTelefone("(83) 98888-1111");
            ana.setCpf("738.591.430-69");
            ana.setDataNascimento(LocalDate.of(1992, 4, 12));
            ana.setCidadeUsuario("João Pessoa");
            ana.setEstadoUsuario("PB");
            ana.setTipo(TipoUsuario.CLIENTE);
            ana.setAtivo(true);
            usuarioRepository.save(ana);

            // Profissional
            Usuario userCarlos = new Usuario();
            userCarlos.setNome("Carlos Silva");
            userCarlos.setEmail("carlos@email.com");
            userCarlos.setSenha(passwordEncoder.encode("123456"));
            userCarlos.setTelefone("(83) 95555-1001");
            userCarlos.setCpf("295.631.480-70");
            userCarlos.setDataNascimento(LocalDate.of(1980, 3, 15));
            userCarlos.setCidadeUsuario("João Pessoa");
            userCarlos.setEstadoUsuario("PB");
            userCarlos.setTipo(TipoUsuario.PROFISSIONAL);
            userCarlos.setAtivo(true);
            usuarioRepository.save(userCarlos);

            Categoria eletricista = categoriaRepository.findAll().stream()
                .filter(c -> c.getNome().equals("Eletricista")).findFirst().orElse(null);

            Profissional carlos = new Profissional();
            carlos.setUsuario(userCarlos);
            carlos.setCategoria(eletricista);
            carlos.setCidade("João Pessoa");
            carlos.setEstado("PB");
            carlos.setPrecoBase(new BigDecimal("80.00"));
            carlos.setDescricao("Eletricista especializado em instalações residenciais e comerciais. +15 anos de experiência.");
            carlos.setDisponivel(true);
            carlos.setVerificado(true);
            carlos.setAvaliacaoMedia(new BigDecimal("4.9"));
            carlos.setQuantidadeAvaliacoes(127);
            carlos.setTokens(45);
            profissionalRepository.save(carlos);

            log.info("Seed concluído: admin@servicematch.com / ana@email.com / carlos@email.com (senha: 123456)");
        };
    }
}
