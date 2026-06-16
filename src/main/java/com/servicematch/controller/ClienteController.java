package com.servicematch.controller;

import com.servicematch.model.*;
import com.servicematch.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/cliente")
@RequiredArgsConstructor
public class ClienteController {

    private static final Logger log = LoggerFactory.getLogger(ClienteController.class);
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/";
    private static final long MAX_UPLOAD_SIZE = 5 * 1024 * 1024;
    private static final Set<String> EXTENSOES_IMAGEM = Set.of(".jpg", ".jpeg", ".png", ".webp");

    private final UsuarioRepository usuarioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ChamadoRepository chamadoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final PasswordEncoder passwordEncoder;

    private Usuario getUsuarioLogado(UserDetails userDetails) {
        return usuarioRepository.findByEmail(userDetails.getUsername()).orElseThrow();
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Usuario usuario = getUsuarioLogado(userDetails);
        List<Chamado> chamados = chamadoRepository.findByClienteIdOrderByDataAberturaDesc(usuario.getId());
        List<Profissional> destaques = profissionalRepository.findByDisponivelTrue().stream().limit(3).toList();
        List<Categoria> categorias = categoriaRepository.findByAtivaTrue();
        long concluidos = chamados.stream().filter(c -> c.getStatus() == StatusChamado.CONCLUIDO).count();
        long avaliacoesFeitasCount = avaliacaoRepository.findByClienteIdOrderByDataAvaliacaoDesc(usuario.getId()).size();
        model.addAttribute("usuario", usuario);
        model.addAttribute("chamadosRecentes", chamados.stream().limit(5).toList());
        model.addAttribute("profissionaisDestaque", destaques);
        model.addAttribute("categorias", categorias);
        model.addAttribute("totalChamados", chamados.size());
        model.addAttribute("totalConcluidos", concluidos);
        model.addAttribute("totalAvaliacoes", avaliacoesFeitasCount);
        model.addAttribute("currentPage", "dashboard");
        return "cliente/dashboard";
    }

    @GetMapping("/buscar")
    public String buscar(@AuthenticationPrincipal UserDetails userDetails,
                         @RequestParam(required = false) String termo,
                         @RequestParam(required = false) Long categoriaId,
                         @RequestParam(required = false) String cidade,
                         @RequestParam(required = false) Boolean disponivel,
                         Model model) {
        List<Profissional> profissionais;
        if (termo != null && !termo.isBlank()) {
            profissionais = profissionalRepository.buscarPorTermo(termo);
        } else {
            profissionais = profissionalRepository.buscarComFiltros(categoriaId, cidade, disponivel);
        }
        model.addAttribute("usuario", getUsuarioLogado(userDetails));
        model.addAttribute("profissionais", profissionais);
        model.addAttribute("categorias", categoriaRepository.findByAtivaTrue());
        model.addAttribute("termo", termo);
        model.addAttribute("categoriaId", categoriaId);
        model.addAttribute("cidade", cidade);
        model.addAttribute("disponivel", disponivel);
        model.addAttribute("currentPage", "buscar");
        return "cliente/buscar";
    }

    @GetMapping("/chamados")
    public String chamados(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Usuario usuario = getUsuarioLogado(userDetails);
        List<Chamado> chamados = chamadoRepository.findByClienteIdOrderByDataAberturaDesc(usuario.getId());
        model.addAttribute("usuario", usuario);
        model.addAttribute("chamados", chamados);
        model.addAttribute("categorias", categoriaRepository.findByAtivaTrue());
        model.addAttribute("currentPage", "chamados");
        return "cliente/chamados";
    }

    @PostMapping("/chamados/novo")
    public String novoChamado(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam String titulo,
                               @RequestParam String descricao,
                               @RequestParam Long categoriaId,
                               @RequestParam String endereco,
                               @RequestParam String cidade,
                               @RequestParam PrioridadeChamado prioridade,
                               RedirectAttributes redirectAttributes) {
        Usuario usuario = getUsuarioLogado(userDetails);
        Chamado chamado = new Chamado();
        chamado.setTitulo(titulo);
        chamado.setDescricao(descricao);
        chamado.setCliente(usuario);
        chamado.setEndereco(endereco);
        chamado.setCidade(cidade);
        chamado.setPrioridade(prioridade);
        chamado.setStatus(StatusChamado.ABERTO);
        categoriaRepository.findById(categoriaId).ifPresent(chamado::setCategoria);
        chamadoRepository.save(chamado);
        redirectAttributes.addFlashAttribute("msg", "Chamado aberto com sucesso!");
        return "redirect:/cliente/chamados";
    }

    @PostMapping("/chamados/{id}/editar")
    public String editarChamado(@AuthenticationPrincipal UserDetails userDetails,
                                 @PathVariable Long id,
                                 @RequestParam String titulo,
                                 @RequestParam String descricao,
                                 @RequestParam Long categoriaId,
                                 @RequestParam PrioridadeChamado prioridade,
                                 @RequestParam String endereco,
                                 @RequestParam String cidade,
                                 RedirectAttributes redirectAttributes) {
        Usuario usuario = getUsuarioLogado(userDetails);
        Chamado chamado = chamadoRepository.findById(id).orElseThrow();
        if (!chamado.getCliente().getId().equals(usuario.getId()) || chamado.getStatus() != StatusChamado.ABERTO) {
            redirectAttributes.addFlashAttribute("erro", "Nao e possivel editar este chamado.");
            return "redirect:/cliente/chamados";
        }
        chamado.setTitulo(titulo);
        chamado.setDescricao(descricao);
        chamado.setEndereco(endereco);
        chamado.setCidade(cidade);
        chamado.setPrioridade(prioridade);
        categoriaRepository.findById(categoriaId).ifPresent(chamado::setCategoria);
        chamadoRepository.save(chamado);
        redirectAttributes.addFlashAttribute("msg", "Chamado atualizado com sucesso!");
        return "redirect:/cliente/chamados";
    }

    @PostMapping("/chamados/{id}/cancelar")
    public String cancelarChamado(@AuthenticationPrincipal UserDetails userDetails,
                                   @PathVariable Long id,
                                   RedirectAttributes redirectAttributes) {
        Usuario usuario = getUsuarioLogado(userDetails);
        Chamado chamado = chamadoRepository.findById(id).orElseThrow();
        if (!chamado.getCliente().getId().equals(usuario.getId()) || chamado.getStatus() != StatusChamado.ABERTO) {
            redirectAttributes.addFlashAttribute("erro", "Nao e possivel cancelar este chamado.");
            return "redirect:/cliente/chamados";
        }
        chamado.setStatus(StatusChamado.CANCELADO);
        chamadoRepository.save(chamado);
        redirectAttributes.addFlashAttribute("msg", "Chamado cancelado.");
        return "redirect:/cliente/chamados";
    }

    @GetMapping("/avaliacoes")
    public String avaliacoes(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Usuario usuario = getUsuarioLogado(userDetails);
        List<Avaliacao> avaliacoes = avaliacaoRepository.findByClienteIdOrderByDataAvaliacaoDesc(usuario.getId());
        List<Chamado> chamadosSemAvaliacao = chamadoRepository
                .findByClienteIdOrderByDataAberturaDesc(usuario.getId())
                .stream()
                .filter(c -> c.getStatus() == StatusChamado.CONCLUIDO
                        && !avaliacaoRepository.existsByChamadoId(c.getId()))
                .toList();
        model.addAttribute("usuario", usuario);
        model.addAttribute("avaliacoes", avaliacoes);
        model.addAttribute("chamadosSemAvaliacao", chamadosSemAvaliacao);
        model.addAttribute("currentPage", "avaliacoes");
        return "cliente/avaliacoes";
    }

    @PostMapping("/avaliacoes/nova")
    public String novaAvaliacao(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam Long chamadoId,
                                 @RequestParam Integer nota,
                                 @RequestParam(required = false) String comentario,
                                 RedirectAttributes redirectAttributes) {
        Usuario usuario = getUsuarioLogado(userDetails);
        Chamado chamado = chamadoRepository.findById(chamadoId).orElseThrow();
        // IDOR: garante que o chamado pertence ao cliente logado
        if (!chamado.getCliente().getId().equals(usuario.getId())) {
            log.warn("IDOR bloqueado: cliente id={} tentou avaliar chamado id={} de outro cliente.", usuario.getId(), chamadoId);
            redirectAttributes.addFlashAttribute("erro", "Acesso negado.");
            return "redirect:/cliente/avaliacoes";
        }
        if (chamado.getStatus() != StatusChamado.CONCLUIDO) {
            log.warn("Cliente id={} tentou avaliar chamado id={} com status {}", usuario.getId(), chamadoId, chamado.getStatus());
            redirectAttributes.addFlashAttribute("erro", "Só é possível avaliar chamados concluídos.");
            return "redirect:/cliente/avaliacoes";
        }
        if (nota < 1 || nota > 5) {
            log.warn("Cliente id={} enviou nota inválida: {}", usuario.getId(), nota);
            redirectAttributes.addFlashAttribute("erro", "Nota inválida.");
            return "redirect:/cliente/avaliacoes";
        }
        if (avaliacaoRepository.existsByChamadoId(chamadoId)) {
            redirectAttributes.addFlashAttribute("erro", "Este chamado ja foi avaliado.");
            return "redirect:/cliente/avaliacoes";
        }
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setChamado(chamado);
        avaliacao.setProfissional(chamado.getProfissional());
        avaliacao.setCliente(usuario);
        avaliacao.setNota(nota);
        avaliacao.setComentario(comentario);
        avaliacaoRepository.save(avaliacao);
        Profissional prof = chamado.getProfissional();
        if (prof != null) {
            long total = avaliacaoRepository.countByProfissionalId(prof.getId());
            // Média Móvel Exponencial (EMA): α=0.3 → 30% de peso para a avaliação mais recente
            double alpha = 0.3;
            double mediaAtual = prof.getAvaliacaoMedia() != null ? prof.getAvaliacaoMedia().doubleValue() : 0.0;
            double novaMedia = (total == 1) ? nota : (alpha * nota + (1 - alpha) * mediaAtual);
            prof.setAvaliacaoMedia(new java.math.BigDecimal(novaMedia).setScale(2, java.math.RoundingMode.HALF_UP));
            prof.setQuantidadeAvaliacoes((int) total);
            profissionalRepository.save(prof);
        }
        redirectAttributes.addFlashAttribute("msg", "Avaliacao enviada com sucesso!");
        return "redirect:/cliente/avaliacoes";
    }

    @GetMapping("/perfil")
    public String perfil(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Usuario usuario = getUsuarioLogado(userDetails);
        long totalChamados = chamadoRepository.findByClienteIdOrderByDataAberturaDesc(usuario.getId()).size();
        long totalAvaliacoes = avaliacaoRepository.findByClienteIdOrderByDataAvaliacaoDesc(usuario.getId()).size();
        model.addAttribute("usuario", usuario);
        model.addAttribute("totalChamados", totalChamados);
        model.addAttribute("totalAvaliacoes", totalAvaliacoes);
        model.addAttribute("currentPage", "perfil");
        return "cliente/perfil";
    }

    @PostMapping("/perfil/foto")
    public String uploadFoto(@AuthenticationPrincipal UserDetails userDetails,
                             @RequestParam MultipartFile foto,
                             RedirectAttributes redirectAttributes) {
        if (foto.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Nenhum arquivo selecionado.");
            return "redirect:/cliente/perfil";
        }
        try {
            if (foto.getSize() > MAX_UPLOAD_SIZE) {
                throw new IllegalArgumentException("Arquivo muito grande. Máximo 5 MB.");
            }
            String orig = foto.getOriginalFilename() != null ? foto.getOriginalFilename() : "";
            String ext = orig.contains(".") ? orig.substring(orig.lastIndexOf('.')).toLowerCase() : "";
            if (!EXTENSOES_IMAGEM.contains(ext)) {
                throw new IllegalArgumentException("Tipo não permitido. Use JPG, PNG ou WebP.");
            }
            Usuario usuario = getUsuarioLogado(userDetails);
            String nome = "cliente_" + usuario.getId() + "_" + UUID.randomUUID() + ext;
            Path path = Paths.get(UPLOAD_DIR + nome);
            Files.createDirectories(path.getParent());
            Files.write(path, foto.getBytes());
            usuario.setFotoUrl("/uploads/" + nome);
            usuarioRepository.save(usuario);
            redirectAttributes.addFlashAttribute("msg", "Foto de perfil atualizada!");
        } catch (IllegalArgumentException e) {
            log.warn("Upload de foto do cliente rejeitado: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        } catch (IOException e) {
            log.error("Erro ao salvar foto do cliente: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar a foto.");
        }
        return "redirect:/cliente/perfil";
    }

    @PostMapping("/perfil/atualizar")
    public String atualizarPerfil(@AuthenticationPrincipal UserDetails userDetails,
                                   @RequestParam String nome,
                                   @RequestParam(required = false) String telefone,
                                   @RequestParam(required = false) String dataNascimento,
                                   @RequestParam(required = false) String endereco,
                                   @RequestParam(required = false) String bairro,
                                   @RequestParam(required = false) String cidadeUsuario,
                                   @RequestParam(required = false) String estadoUsuario,
                                   @RequestParam(required = false) String cep,
                                   RedirectAttributes redirectAttributes) {
        Usuario usuario = getUsuarioLogado(userDetails);
        usuario.setNome(nome);
        usuario.setTelefone(telefone);
        // CPF não é alterado pelo próprio usuário — somente o admin pode alterar
        if (dataNascimento != null && !dataNascimento.isBlank()) {
            usuario.setDataNascimento(LocalDate.parse(dataNascimento));
        }
        usuario.setEndereco(endereco);
        usuario.setBairro(bairro);
        usuario.setCidadeUsuario(cidadeUsuario);
        usuario.setEstadoUsuario(estadoUsuario);
        usuario.setCep(cep);
        usuarioRepository.save(usuario);
        redirectAttributes.addFlashAttribute("msg", "Perfil atualizado com sucesso!");
        return "redirect:/cliente/perfil";
    }

    @PostMapping("/perfil/senha")
    public String alterarSenha(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam String senhaAtual,
                                @RequestParam String novaSenha,
                                @RequestParam String confirmarSenha,
                                RedirectAttributes redirectAttributes) {
        Usuario usuario = getUsuarioLogado(userDetails);
        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            redirectAttributes.addFlashAttribute("erro", "Senha atual incorreta.");
            return "redirect:/cliente/perfil";
        }
        if (novaSenha.length() < 6) {
            redirectAttributes.addFlashAttribute("erro", "A nova senha deve ter pelo menos 6 caracteres.");
            return "redirect:/cliente/perfil";
        }
        if (!novaSenha.equals(confirmarSenha)) {
            redirectAttributes.addFlashAttribute("erro", "As novas senhas nao coincidem.");
            return "redirect:/cliente/perfil";
        }
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
        redirectAttributes.addFlashAttribute("msg", "Senha alterada com sucesso!");
        return "redirect:/cliente/perfil";
    }
}
