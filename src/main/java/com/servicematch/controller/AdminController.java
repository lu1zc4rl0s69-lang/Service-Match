package com.servicematch.controller;

import com.servicematch.model.*;
import com.servicematch.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ChamadoRepository chamadoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final CategoriaRepository categoriaRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalClientes", usuarioRepository.findByTipo(TipoUsuario.CLIENTE).size());
        model.addAttribute("totalProfissionais", profissionalRepository.count());
        model.addAttribute("totalChamados", chamadoRepository.count());
        model.addAttribute("chamadosAbertos", chamadoRepository.countByStatus(StatusChamado.ABERTO));
        model.addAttribute("chamadosAndamento", chamadoRepository.countByStatus(StatusChamado.EM_ANDAMENTO));
        model.addAttribute("chamadosConcluidos", chamadoRepository.countByStatus(StatusChamado.CONCLUIDO));
        model.addAttribute("chamadosRecentes", chamadoRepository.findByStatusOrderByDataAberturaDesc(StatusChamado.ABERTO).stream().limit(5).toList());
        model.addAttribute("profissionaisRecentes", profissionalRepository.findAll().stream().limit(5).toList());
        model.addAttribute("currentPage", "dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/clientes")
    public String clientes(Model model) {
        model.addAttribute("clientes", usuarioRepository.findByTipo(TipoUsuario.CLIENTE));
        model.addAttribute("currentPage", "clientes");
        return "admin/clientes";
    }

    @PostMapping("/clientes/{id}/toggle")
    public String toggleCliente(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow();
        usuario.setAtivo(!usuario.isAtivo());
        usuarioRepository.save(usuario);
        redirectAttributes.addFlashAttribute("msg", usuario.isAtivo() ? "Cliente ativado." : "Cliente desativado.");
        return "redirect:/admin/clientes";
    }

    @GetMapping("/profissionais")
    public String profissionais(Model model) {
        model.addAttribute("profissionais", profissionalRepository.findAll());
        model.addAttribute("currentPage", "profissionais");
        return "admin/profissionais";
    }

    @PostMapping("/profissionais/{id}/verificar")
    public String verificarProfissional(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Profissional profissional = profissionalRepository.findById(id).orElseThrow();
        profissional.setVerificado(!profissional.isVerificado());
        profissionalRepository.save(profissional);
        redirectAttributes.addFlashAttribute("msg", profissional.isVerificado() ? "Profissional verificado." : "Verificacao removida.");
        return "redirect:/admin/profissionais";
    }

    @GetMapping("/categorias")
    public String categorias(Model model) {
        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("currentPage", "categorias");
        return "admin/categorias";
    }

    @PostMapping("/categorias/nova")
    public String novaCategoria(@RequestParam String nome,
                                 @RequestParam(required = false) String icone,
                                 RedirectAttributes redirectAttributes) {
        if (categoriaRepository.existsByNomeIgnoreCase(nome)) {
            redirectAttributes.addFlashAttribute("erro", "Categoria ja existe.");
            return "redirect:/admin/categorias";
        }
        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        categoria.setIcone(icone != null ? icone : "🔧");
        categoriaRepository.save(categoria);
        redirectAttributes.addFlashAttribute("msg", "Categoria criada com sucesso!");
        return "redirect:/admin/categorias";
    }

    @PostMapping("/categorias/{id}/toggle")
    public String toggleCategoria(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Categoria categoria = categoriaRepository.findById(id).orElseThrow();
        categoria.setAtiva(!categoria.isAtiva());
        categoriaRepository.save(categoria);
        redirectAttributes.addFlashAttribute("msg", "Categoria atualizada.");
        return "redirect:/admin/categorias";
    }

    @GetMapping("/avaliacoes")
    public String avaliacoes(Model model) {
        model.addAttribute("avaliacoes", avaliacaoRepository.findAll());
        model.addAttribute("currentPage", "avaliacoes");
        return "admin/avaliacoes";
    }

    @PostMapping("/avaliacoes/{id}/excluir")
    public String excluirAvaliacao(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        avaliacaoRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("msg", "Avaliacao removida.");
        return "redirect:/admin/avaliacoes";
    }
}
