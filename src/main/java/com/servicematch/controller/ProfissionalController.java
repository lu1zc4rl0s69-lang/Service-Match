package com.servicematch.controller;

import com.servicematch.model.*;
import com.servicematch.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/profissional")
@RequiredArgsConstructor
public class ProfissionalController {

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

    @GetMapping("/publico/{id}")
    public String perfilPublico(@PathVariable Long id, Model model) {
        Profissional profissional = profissionalRepository.findById(id).orElseThrow();
        List<Avaliacao> avaliacoes = avaliacaoRepository.findByProfissionalIdOrderByDataAvaliacaoDesc(id);
        model.addAttribute("profissional", profissional);
        model.addAttribute("avaliacoes", avaliacoes);
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
        if (profissional.getTokens() < 10) {
            redirectAttributes.addFlashAttribute("erro", "Saldo insuficiente de tokens.");
            return "redirect:/profissional/chamados";
        }
        Chamado chamado = chamadoRepository.findById(id).orElseThrow();
        chamado.setProfissional(profissional);
        chamado.setStatus(StatusChamado.EM_ANDAMENTO);
        chamadoRepository.save(chamado);
        profissional.setTokens(profissional.getTokens() - 10);
        profissionalRepository.save(profissional);
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
    public String concluirChamado(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Chamado chamado = chamadoRepository.findById(id).orElseThrow();
        chamado.setStatus(StatusChamado.CONCLUIDO);
        chamado.setDataConclusao(java.time.LocalDateTime.now());
        chamadoRepository.save(chamado);
        redirectAttributes.addFlashAttribute("msg", "Chamado concluido com sucesso!");
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
                                 @RequestParam BigDecimal valorPago,
                                 RedirectAttributes redirectAttributes) {
        Profissional profissional = getProfissionalLogado(userDetails);
        profissional.setTokens(profissional.getTokens() + quantidade);
        profissionalRepository.save(profissional);
        TokenMovimento mov = new TokenMovimento();
        mov.setProfissional(profissional);
        mov.setTipo("COMPRA");
        mov.setQuantidade(quantidade);
        mov.setValorPago(valorPago);
        mov.setDescricao("Compra de pacote de " + quantidade + " tokens");
        tokenMovimentoRepository.save(mov);
        redirectAttributes.addFlashAttribute("msg", quantidade + " tokens adicionados com sucesso!");
        return "redirect:/profissional/tokens";
    }

    @GetMapping("/perfil")
    public String perfil(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Profissional profissional = getProfissionalLogado(userDetails);
        model.addAttribute("profissional", profissional);
        model.addAttribute("categorias", categoriaRepository.findByAtivaTrue());
        model.addAttribute("currentPage", "perfil");
        return "profissional/perfil";
    }

    @PostMapping("/perfil/atualizar")
    public String atualizarPerfil(@AuthenticationPrincipal UserDetails userDetails,
                                   @RequestParam String nome,
                                   @RequestParam String telefone,
                                   @RequestParam String cidade,
                                   @RequestParam String estado,
                                   @RequestParam BigDecimal precoBase,
                                   @RequestParam String descricao,
                                   @RequestParam Long categoriaId,
                                   @RequestParam(defaultValue = "false") boolean disponivel,
                                   RedirectAttributes redirectAttributes) {
        Profissional profissional = getProfissionalLogado(userDetails);
        profissional.getUsuario().setNome(nome);
        profissional.getUsuario().setTelefone(telefone);
        profissional.setCidade(cidade);
        profissional.setEstado(estado);
        profissional.setPrecoBase(precoBase);
        profissional.setDescricao(descricao);
        profissional.setDisponivel(disponivel);
        categoriaRepository.findById(categoriaId).ifPresent(profissional::setCategoria);
        usuarioRepository.save(profissional.getUsuario());
        profissionalRepository.save(profissional);
        redirectAttributes.addFlashAttribute("msg", "Perfil atualizado com sucesso!");
        return "redirect:/profissional/perfil";
    }
}
