package com.servicematch.controller;

import com.servicematch.model.*;
import com.servicematch.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/profissional")
@RequiredArgsConstructor
public class ProfissionalController {

    private static final Logger log = LoggerFactory.getLogger(ProfissionalController.class);

    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/";
    private static final long MAX_UPLOAD_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final java.util.Set<String> EXTENSOES_IMAGEM =
            java.util.Set.of(".jpg", ".jpeg", ".png", ".webp");

    // Pacotes de tokens fixos no servidor — client não pode alterar preço
    private static final java.util.Map<Integer, java.math.BigDecimal> PACOTES_TOKEN =
            java.util.Map.of(
                10,  new java.math.BigDecimal("29.90"),
                25,  new java.math.BigDecimal("59.90"),
                50,  new java.math.BigDecimal("99.90"),
                100, new java.math.BigDecimal("179.90")
            );

    private final UsuarioRepository usuarioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ChamadoRepository chamadoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final TokenMovimentoRepository tokenMovimentoRepository;

    private Usuario getUsuarioLogado(UserDetails userDetails) {
        return usuarioRepository.findByEmail(userDetails.getUsername()).orElseThrow();
    }

    private Profissional getProfissionalLogado(UserDetails userDetails) {
        Usuario usuario = getUsuarioLogado(userDetails);
        return profissionalRepository.findByUsuarioId(usuario.getId()).orElseThrow();
    }

    private String salvarArquivo(MultipartFile file, String prefix) throws IOException {
        if (file.getSize() > MAX_UPLOAD_SIZE) {
            throw new IllegalArgumentException("Arquivo muito grande. Máximo 5 MB.");
        }
        String orig = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String ext = orig.contains(".") ? orig.substring(orig.lastIndexOf('.')).toLowerCase() : "";
        if (!EXTENSOES_IMAGEM.contains(ext)) {
            throw new IllegalArgumentException("Tipo não permitido. Use JPG, PNG ou WebP.");
        }
        // UUID garante que o nome no disco nunca tem relação com o nome original
        String nome = prefix + "_" + UUID.randomUUID() + ext;
        Path path = Paths.get(UPLOAD_DIR + nome);
        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());
        return "/uploads/" + nome;
    }

    private List<String> parsePortfolio(String raw) {
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(raw.split(",")));
    }

    @GetMapping("/publico/{id}")
    public String perfilPublico(@PathVariable Long id, Model model) {
        Profissional profissional = profissionalRepository.findById(id).orElseThrow();
        List<Avaliacao> avaliacoes = avaliacaoRepository.findByProfissionalIdOrderByDataAvaliacaoDesc(id);
        model.addAttribute("profissional", profissional);
        model.addAttribute("avaliacoes", avaliacoes);
        model.addAttribute("portfolioList", parsePortfolio(profissional.getPortfolioFotos()));
        return "profissional/perfil-publico";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Profissional profissional = getProfissionalLogado(userDetails);
        List<Chamado> chamadosAtivos = chamadoRepository
                .findByProfissionalIdOrderByDataAberturaDesc(profissional.getId())
                .stream()
                .filter(c -> c.getStatus() == StatusChamado.EM_ANDAMENTO || c.getStatus() == StatusChamado.ABERTO)
                .limit(5).toList();
        List<Avaliacao> avaliacoesRecentes = avaliacaoRepository
                .findByProfissionalIdOrderByDataAvaliacaoDesc(profissional.getId())
                .stream().limit(3).toList();
        long totalConcluidos = chamadoRepository.countByProfissionalId(profissional.getId());
        model.addAttribute("profissional", profissional);
        model.addAttribute("chamadosAtivos", chamadosAtivos);
        model.addAttribute("avaliacoesRecentes", avaliacoesRecentes);
        model.addAttribute("totalConcluidos", totalConcluidos);
        model.addAttribute("currentPage", "dashboard");
        return "profissional/dashboard";
    }

    @GetMapping("/chamados")
    public String chamados(@AuthenticationPrincipal UserDetails userDetails,
                            @RequestParam(required = false) Long categoriaId,
                            Model model) {
        Profissional profissional = getProfissionalLogado(userDetails);
        List<Chamado> disponiveis;
        if (categoriaId != null) {
            disponiveis = chamadoRepository.findByCategoriaIdAndStatus(categoriaId, StatusChamado.ABERTO);
        } else {
            disponiveis = chamadoRepository.findByStatusOrderByDataAberturaDesc(StatusChamado.ABERTO);
        }
        List<Chamado> meusChamados = chamadoRepository.findByProfissionalIdOrderByDataAberturaDesc(profissional.getId());
        model.addAttribute("profissional", profissional);
        model.addAttribute("chamadosDisponiveis", disponiveis);
        model.addAttribute("meusChamados", meusChamados);
        model.addAttribute("categorias", categoriaRepository.findByAtivaTrue());
        model.addAttribute("categoriaId", categoriaId);
        model.addAttribute("currentPage", "chamados");
        return "profissional/chamados";
    }

    @PostMapping("/chamados/{id}/aceitar")
    public String aceitarChamado(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        Profissional profissional = getProfissionalLogado(userDetails);
        Chamado chamado = chamadoRepository.findById(id).orElseThrow();
        // Chamado deve estar ABERTO — evita que dois profissionais aceitem o mesmo
        if (chamado.getStatus() != StatusChamado.ABERTO) {
            log.warn("Profissional id={} tentou aceitar chamado id={} com status {}", profissional.getId(), id, chamado.getStatus());
            redirectAttributes.addFlashAttribute("erro", "Este chamado não está mais disponível.");
            return "redirect:/profissional/chamados";
        }
        if (profissional.getTokens() < 10) {
            redirectAttributes.addFlashAttribute("erro", "Saldo insuficiente de tokens.");
            return "redirect:/profissional/chamados";
        }
        chamado.setProfissional(profissional);
        chamado.setStatus(StatusChamado.EM_ANDAMENTO);
        chamadoRepository.save(chamado);
        profissional.setTokens(profissional.getTokens() - 10);
        profissionalRepository.save(profissional);
        log.info("Profissional id={} aceitou chamado id={} (-10 tokens).", profissional.getId(), id);
        TokenMovimento mov = new TokenMovimento();
        mov.setProfissional(profissional);
        mov.setTipo("DESBLOQUEIO");
        mov.setQuantidade(-10);
        mov.setDescricao("Desbloqueio do chamado: " + chamado.getTitulo());
        tokenMovimentoRepository.save(mov);
        redirectAttributes.addFlashAttribute("msg", "Chamado aceito! Contato do cliente liberado.");
        return "redirect:/profissional/chamados";
    }

    @PostMapping("/chamados/{id}/concluir")
    public String concluirChamado(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        Profissional profissional = getProfissionalLogado(userDetails);
        Chamado chamado = chamadoRepository.findById(id).orElseThrow();
        // Só o profissional dono do chamado pode concluí-lo
        if (chamado.getProfissional() == null || !chamado.getProfissional().getId().equals(profissional.getId())) {
            log.warn("IDOR bloqueado: profissional id={} tentou concluir chamado id={} de outro profissional.", profissional.getId(), id);
            redirectAttributes.addFlashAttribute("erro", "Acesso negado.");
            return "redirect:/profissional/chamados";
        }
        if (chamado.getStatus() != StatusChamado.EM_ANDAMENTO) {
            log.warn("Profissional id={} tentou concluir chamado id={} com status {}", profissional.getId(), id, chamado.getStatus());
            redirectAttributes.addFlashAttribute("erro", "Apenas chamados em andamento podem ser concluídos.");
            return "redirect:/profissional/chamados";
        }
        chamado.setStatus(StatusChamado.CONCLUIDO);
        chamado.setDataConclusao(java.time.LocalDateTime.now());
        chamadoRepository.save(chamado);
        redirectAttributes.addFlashAttribute("msg", "Chamado concluído com sucesso!");
        return "redirect:/profissional/chamados";
    }

    @GetMapping("/avaliacoes")
    public String avaliacoes(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Profissional profissional = getProfissionalLogado(userDetails);
        List<Avaliacao> avaliacoes = avaliacaoRepository.findByProfissionalIdOrderByDataAvaliacaoDesc(profissional.getId());
        model.addAttribute("profissional", profissional);
        model.addAttribute("avaliacoes", avaliacoes);
        model.addAttribute("currentPage", "avaliacoes");
        return "profissional/avaliacoes";
    }

    @GetMapping("/tokens")
    public String tokens(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Profissional profissional = getProfissionalLogado(userDetails);
        List<TokenMovimento> historico = tokenMovimentoRepository.findByProfissionalIdOrderByDataMovimentoDesc(profissional.getId());
        model.addAttribute("profissional", profissional);
        model.addAttribute("historico", historico);
        model.addAttribute("currentPage", "tokens");
        return "profissional/tokens";
    }

    @PostMapping("/tokens/comprar")
    public String comprarTokens(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam Integer quantidade,
                                 RedirectAttributes redirectAttributes) {
        // Valor determinado pelo servidor com base no pacote — nunca pelo cliente
        java.math.BigDecimal valorCorreto = PACOTES_TOKEN.get(quantidade);
        if (valorCorreto == null) {
            log.warn("Tentativa de compra com pacote inválido: quantidade={}", quantidade);
            redirectAttributes.addFlashAttribute("erro", "Pacote de tokens inválido.");
            return "redirect:/profissional/tokens";
        }
        Profissional profissional = getProfissionalLogado(userDetails);
        profissional.setTokens(profissional.getTokens() + quantidade);
        profissionalRepository.save(profissional);
        TokenMovimento mov = new TokenMovimento();
        mov.setProfissional(profissional);
        mov.setTipo("COMPRA");
        mov.setQuantidade(quantidade);
        mov.setValorPago(valorCorreto);
        mov.setDescricao("Compra de pacote de " + quantidade + " tokens");
        tokenMovimentoRepository.save(mov);
        log.info("Profissional id={} comprou {} tokens por R${}", profissional.getId(), quantidade, valorCorreto);
        redirectAttributes.addFlashAttribute("msg", quantidade + " tokens adicionados com sucesso!");
        return "redirect:/profissional/tokens";
    }

    @GetMapping("/perfil")
    public String perfil(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Profissional profissional = getProfissionalLogado(userDetails);
        model.addAttribute("profissional", profissional);
        model.addAttribute("categorias", categoriaRepository.findByAtivaTrue());
        model.addAttribute("portfolioList", parsePortfolio(profissional.getPortfolioFotos()));
        model.addAttribute("currentPage", "perfil");
        return "profissional/perfil";
    }

    @PostMapping("/perfil/atualizar")
    public String atualizarPerfil(@AuthenticationPrincipal UserDetails userDetails,
                                   @RequestParam String nome,
                                   @RequestParam(required = false) String telefone,
                                   @RequestParam(required = false) String cidade,
                                   @RequestParam(required = false) String estado,
                                   @RequestParam(required = false) BigDecimal precoBase,
                                   @RequestParam(required = false) String descricao,
                                   @RequestParam Long categoriaId,
                                   @RequestParam(required = false) Boolean disponivel,
                                   @RequestParam(required = false) String servicos,
                                   RedirectAttributes redirectAttributes) {
        Profissional profissional = getProfissionalLogado(userDetails);
        profissional.getUsuario().setNome(nome);
        profissional.getUsuario().setTelefone(telefone);
        profissional.setCidade(cidade);
        profissional.setEstado(estado);
        profissional.setPrecoBase(precoBase);
        profissional.setDescricao(descricao);
        profissional.setDisponivel(Boolean.TRUE.equals(disponivel));
        profissional.setServicos(servicos);
        categoriaRepository.findById(categoriaId).ifPresent(profissional::setCategoria);
        usuarioRepository.save(profissional.getUsuario());
        profissionalRepository.save(profissional);
        redirectAttributes.addFlashAttribute("msg", "Perfil atualizado com sucesso!");
        return "redirect:/profissional/perfil";
    }

    @PostMapping("/perfil/foto")
    public String uploadFoto(@AuthenticationPrincipal UserDetails userDetails,
                              @RequestParam MultipartFile foto,
                              RedirectAttributes redirectAttributes) {
        if (foto.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Nenhum arquivo selecionado.");
            return "redirect:/profissional/perfil";
        }
        try {
            Profissional profissional = getProfissionalLogado(userDetails);
            String url = salvarArquivo(foto, "foto_" + profissional.getId());
            profissional.setFotoUrl(url);
            profissionalRepository.save(profissional);
            redirectAttributes.addFlashAttribute("msg", "Foto de perfil atualizada!");
        } catch (IllegalArgumentException e) {
            log.warn("Upload de foto rejeitado: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        } catch (IOException e) {
            log.error("Erro ao salvar foto de perfil: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar a foto.");
        }
        return "redirect:/profissional/perfil";
    }

    @PostMapping("/perfil/portfolio/add")
    public String addPortfolio(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam MultipartFile foto,
                                RedirectAttributes redirectAttributes) {
        if (foto.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Nenhum arquivo selecionado.");
            return "redirect:/profissional/perfil";
        }
        try {
            Profissional profissional = getProfissionalLogado(userDetails);
            List<String> lista = parsePortfolio(profissional.getPortfolioFotos());
            if (lista.size() >= 6) {
                redirectAttributes.addFlashAttribute("erro", "Limite de 6 fotos no portfólio atingido.");
                return "redirect:/profissional/perfil";
            }
            String url = salvarArquivo(foto, "portfolio_" + profissional.getId());
            lista.add(url);
            profissional.setPortfolioFotos(String.join(",", lista));
            profissionalRepository.save(profissional);
            redirectAttributes.addFlashAttribute("msg", "Foto adicionada ao portfólio!");
        } catch (IllegalArgumentException e) {
            log.warn("Upload de portfólio rejeitado: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        } catch (IOException e) {
            log.error("Erro ao salvar foto de portfólio: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar a foto.");
        }
        return "redirect:/profissional/perfil";
    }

    @PostMapping("/perfil/portfolio/remove")
    public String removePortfolio(@AuthenticationPrincipal UserDetails userDetails,
                                   @RequestParam String url,
                                   RedirectAttributes redirectAttributes) {
        Profissional profissional = getProfissionalLogado(userDetails);
        List<String> lista = parsePortfolio(profissional.getPortfolioFotos());
        lista.remove(url);
        profissional.setPortfolioFotos(lista.isEmpty() ? null : String.join(",", lista));
        profissionalRepository.save(profissional);
        redirectAttributes.addFlashAttribute("msg", "Foto removida do portfólio.");
        return "redirect:/profissional/perfil";
    }
}
