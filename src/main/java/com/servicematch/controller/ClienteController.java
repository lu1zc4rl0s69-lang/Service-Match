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

import java.util.List;

@Controller
@RequestMapping("/cliente")
@RequiredArgsConstructor
public class ClienteController {

    private final UsuarioRepository usuarioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ChamadoRepository chamadoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final CategoriaRepository categoriaRepository;

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
    public String buscar(@RequestParam(required = false) String termo,
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
            Double media = avaliacaoRepository.calcularMediaPorProfissional(prof.getId());
            long total = avaliacaoRepository.countByProfissionalId(prof.getId());
            prof.setAvaliacaoMedia(media != null ? new java.math.BigDecimal(media).setScale(1, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.ZERO);
            prof.setQuantidadeAvaliacoes((int) total);
            profissionalRepository.save(prof);
        }
        redirectAttributes.addFlashAttribute("msg", "Avaliacao enviada com sucesso!");
        return "redirect:/cliente/avaliacoes";
    }

    @GetMapping("/perfil")
    public String perfil(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("usuario", getUsuarioLogado(userDetails));
        model.addAttribute("currentPage", "perfil");
        return "cliente/perfil";
    }

    @PostMapping("/perfil/atualizar")
    public String atualizarPerfil(@AuthenticationPrincipal UserDetails userDetails,
                                   @RequestParam String nome,
                                   @RequestParam String telefone,
                                   RedirectAttributes redirectAttributes) {
        Usuario usuario = getUsuarioLogado(userDetails);
        usuario.setNome(nome);
        usuario.setTelefone(telefone);
        usuarioRepository.save(usuario);
        redirectAttributes.addFlashAttribute("msg", "Perfil atualizado com sucesso!");
        return "redirect:/cliente/perfil";
    }
}
