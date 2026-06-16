package com.servicematch.controller;

import com.servicematch.model.*;
import com.servicematch.repository.*;
import com.servicematch.util.CpfUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final UsuarioRepository usuarioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ChamadoRepository chamadoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final com.servicematch.repository.TarifaRepository tarifaRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long profissionaisDisponiveis = profissionalRepository.findByDisponivelTrue().size();

        double mediaGeral = profissionalRepository.findAll().stream()
                .filter(p -> p.getAvaliacaoMedia() != null && p.getQuantidadeAvaliacoes() > 0)
                .mapToDouble(p -> p.getAvaliacaoMedia().doubleValue())
                .average().orElse(0.0);
        java.math.BigDecimal satisfacaoMedia = new java.math.BigDecimal(mediaGeral)
                .setScale(1, java.math.RoundingMode.HALF_UP);

        java.util.List<Chamado> chamadosRecentes = chamadoRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Chamado::getDataAbertura).reversed())
                .limit(6).toList();

        model.addAttribute("totalClientes", usuarioRepository.findByTipo(TipoUsuario.CLIENTE).size());
        model.addAttribute("totalProfissionais", profissionalRepository.count());
        model.addAttribute("totalChamados", chamadoRepository.count());
        model.addAttribute("chamadosAbertos", chamadoRepository.countByStatus(StatusChamado.ABERTO));
        model.addAttribute("chamadosAndamento", chamadoRepository.countByStatus(StatusChamado.EM_ANDAMENTO));
        model.addAttribute("chamadosConcluidos", chamadoRepository.countByStatus(StatusChamado.CONCLUIDO));
        model.addAttribute("profissionaisDisponiveis", profissionaisDisponiveis);
        model.addAttribute("satisfacaoMedia", satisfacaoMedia);
        model.addAttribute("chamadosRecentes", chamadosRecentes);
        model.addAttribute("profissionaisRecentes", profissionalRepository.findAll().stream().limit(5).toList());
        model.addAttribute("currentPage", "dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/clientes")
    public String clientes(@RequestParam(required = false) String termo, Model model) {
        java.util.List<Usuario> todos = usuarioRepository.findByTipo(TipoUsuario.CLIENTE);
        java.util.List<Usuario> clientes = todos;

        if (termo != null && !termo.isBlank()) {
            String t = termo.toLowerCase();
            clientes = todos.stream()
                .filter(u -> u.getNome().toLowerCase().contains(t)
                          || u.getEmail().toLowerCase().contains(t)
                          || (u.getCidadeUsuario() != null && u.getCidadeUsuario().toLowerCase().contains(t)))
                .toList();
        }

        long totalAtivos = clientes.stream().filter(Usuario::isAtivo).count();

        java.util.Set<Long> clienteIds = clientes.stream()
                .map(Usuario::getId).collect(java.util.stream.Collectors.toSet());
        java.util.List<Chamado> todosChamados = chamadoRepository.findAll();
        long totalServicos = todosChamados.stream()
                .filter(c -> clienteIds.contains(c.getCliente().getId())).count();

        java.util.Map<Long, Long> servicosPorCliente = todosChamados.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        c -> c.getCliente().getId(), java.util.stream.Collectors.counting()));

        model.addAttribute("clientes", clientes);
        model.addAttribute("totalClientes", todos.size());
        model.addAttribute("totalAtivos", totalAtivos);
        model.addAttribute("totalServicos", totalServicos);
        model.addAttribute("servicosPorCliente", servicosPorCliente);
        model.addAttribute("termo", termo);
        model.addAttribute("currentPage", "clientes");
        return "admin/clientes";
    }

    @PostMapping("/clientes/novo")
    public String novoCliente(@RequestParam String nome,
                              @RequestParam String email,
                              @RequestParam(required = false) String telefone,
                              @RequestParam String senha,
                              RedirectAttributes redirectAttributes) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            redirectAttributes.addFlashAttribute("erro", "E-mail já cadastrado.");
            return "redirect:/admin/clientes";
        }
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(email);
        u.setTelefone(telefone);
        u.setSenha(passwordEncoder.encode(senha));
        u.setTipo(TipoUsuario.CLIENTE);
        u.setAtivo(true);
        usuarioRepository.save(u);
        redirectAttributes.addFlashAttribute("msg", "Cliente criado com sucesso!");
        return "redirect:/admin/clientes";
    }

    @PostMapping("/clientes/{id}/cpf")
    public String editarCpfCliente(@PathVariable Long id,
                                    @RequestParam String cpf,
                                    RedirectAttributes redirectAttributes) {
        if (!CpfUtils.isValido(cpf)) {
            log.warn("Admin tentou salvar CPF inválido para usuário id={}: '{}'", id, cpf);
            redirectAttributes.addFlashAttribute("erro", "CPF inválido. Verifique os dígitos e tente novamente.");
            return "redirect:/admin/clientes";
        }
        Usuario usuario = usuarioRepository.findById(id).orElseThrow();
        usuario.setCpf(CpfUtils.formatar(cpf));
        usuarioRepository.save(usuario);
        log.info("CPF do usuário id={} atualizado pelo admin.", id);
        redirectAttributes.addFlashAttribute("msg", "CPF de " + usuario.getNome() + " atualizado com sucesso!");
        return "redirect:/admin/clientes";
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
    public String profissionais(@RequestParam(required = false) String termo,
                                @RequestParam(required = false) String status,
                                Model model) {
        java.util.List<Profissional> todos = profissionalRepository.findAll();
        java.util.List<Profissional> profissionais = todos;

        if (termo != null && !termo.isBlank()) {
            String t = termo.toLowerCase();
            profissionais = profissionais.stream()
                .filter(p -> p.getUsuario().getNome().toLowerCase().contains(t)
                          || p.getUsuario().getEmail().toLowerCase().contains(t)
                          || (p.getCategoria() != null && p.getCategoria().getNome().toLowerCase().contains(t)))
                .toList();
        }

        if ("aprovado".equals(status)) {
            profissionais = profissionais.stream()
                .filter(p -> p.isVerificado() && p.getUsuario().isAtivo()).toList();
        } else if ("pendente".equals(status)) {
            profissionais = profissionais.stream()
                .filter(p -> !p.isVerificado() && p.getUsuario().isAtivo()).toList();
        } else if ("rejeitado".equals(status)) {
            profissionais = profissionais.stream()
                .filter(p -> !p.getUsuario().isAtivo()).toList();
        }

        long totalAprovados  = todos.stream().filter(p -> p.isVerificado() && p.getUsuario().isAtivo()).count();
        long totalPendentes  = todos.stream().filter(p -> !p.isVerificado() && p.getUsuario().isAtivo()).count();
        long totalRejeitados = todos.stream().filter(p -> !p.getUsuario().isAtivo()).count();

        model.addAttribute("profissionais", profissionais);
        model.addAttribute("totalProfissionais", todos.size());
        model.addAttribute("totalAprovados", totalAprovados);
        model.addAttribute("totalPendentes", totalPendentes);
        model.addAttribute("totalRejeitados", totalRejeitados);
        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("termo", termo);
        model.addAttribute("statusFiltro", status);
        model.addAttribute("currentPage", "profissionais");
        return "admin/profissionais";
    }

    @PostMapping("/profissionais/{id}/aprovar")
    public String aprovarProfissional(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Profissional p = profissionalRepository.findById(id).orElseThrow();
        p.setVerificado(true);
        p.getUsuario().setAtivo(true);
        usuarioRepository.save(p.getUsuario());
        profissionalRepository.save(p);
        redirectAttributes.addFlashAttribute("msg", "Profissional aprovado.");
        return "redirect:/admin/profissionais";
    }

    @PostMapping("/profissionais/{id}/pendente")
    public String tornarPendente(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Profissional p = profissionalRepository.findById(id).orElseThrow();
        p.setVerificado(false);
        p.getUsuario().setAtivo(true);
        usuarioRepository.save(p.getUsuario());
        profissionalRepository.save(p);
        redirectAttributes.addFlashAttribute("msg", "Profissional marcado como pendente.");
        return "redirect:/admin/profissionais";
    }

    @PostMapping("/profissionais/{id}/rejeitar")
    public String rejeitarProfissional(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Profissional p = profissionalRepository.findById(id).orElseThrow();
        p.setVerificado(false);
        p.getUsuario().setAtivo(false);
        usuarioRepository.save(p.getUsuario());
        profissionalRepository.save(p);
        redirectAttributes.addFlashAttribute("msg", "Profissional rejeitado.");
        return "redirect:/admin/profissionais";
    }

    @PostMapping("/profissionais/novo")
    public String novoProfissional(@RequestParam String nome,
                                   @RequestParam String email,
                                   @RequestParam(required = false) String telefone,
                                   @RequestParam String senha,
                                   @RequestParam(required = false) Long categoriaId,
                                   @RequestParam(required = false) String cidade,
                                   @RequestParam(required = false) String estado,
                                   @RequestParam(required = false) java.math.BigDecimal precoBase,
                                   RedirectAttributes redirectAttributes) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            redirectAttributes.addFlashAttribute("erro", "E-mail já cadastrado.");
            return "redirect:/admin/profissionais";
        }
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(email);
        u.setTelefone(telefone);
        u.setSenha(passwordEncoder.encode(senha));
        u.setTipo(TipoUsuario.PROFISSIONAL);
        u.setAtivo(true);
        usuarioRepository.save(u);

        Profissional p = new Profissional();
        p.setUsuario(u);
        if (categoriaId != null) {
            categoriaRepository.findById(categoriaId).ifPresent(p::setCategoria);
        }
        p.setCidade(cidade);
        p.setEstado(estado);
        p.setPrecoBase(precoBase);
        p.setTokens(0);
        profissionalRepository.save(p);

        redirectAttributes.addFlashAttribute("msg", "Profissional criado com sucesso!");
        return "redirect:/admin/profissionais";
    }

    @PostMapping("/profissionais/{id}/verificar")
    public String verificarProfissional(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Profissional profissional = profissionalRepository.findById(id).orElseThrow();
        profissional.setVerificado(!profissional.isVerificado());
        profissionalRepository.save(profissional);
        redirectAttributes.addFlashAttribute("msg", profissional.isVerificado() ? "Profissional verificado." : "Verificacao removida.");
        return "redirect:/admin/profissionais";
    }

    @PostMapping("/profissionais/{id}/editar")
    public String editarProfissional(@PathVariable Long id,
                                     @RequestParam(required = false) String statusProf,
                                     @RequestParam(required = false) Long categoriaId,
                                     @RequestParam(required = false) String cidade,
                                     @RequestParam(required = false) String estado,
                                     @RequestParam(required = false) java.math.BigDecimal precoBase,
                                     RedirectAttributes redirectAttributes) {
        Profissional p = profissionalRepository.findById(id).orElseThrow();
        if ("aprovado".equals(statusProf)) {
            p.setVerificado(true);
            p.getUsuario().setAtivo(true);
        } else if ("pendente".equals(statusProf)) {
            p.setVerificado(false);
            p.getUsuario().setAtivo(true);
        } else if ("rejeitado".equals(statusProf)) {
            p.setVerificado(false);
            p.getUsuario().setAtivo(false);
        }
        if (categoriaId != null) {
            categoriaRepository.findById(categoriaId).ifPresent(p::setCategoria);
        } else {
            p.setCategoria(null);
        }
        p.setCidade(cidade != null && !cidade.isBlank() ? cidade : p.getCidade());
        p.setEstado(estado != null && !estado.isBlank() ? estado : p.getEstado());
        if (precoBase != null) p.setPrecoBase(precoBase);
        usuarioRepository.save(p.getUsuario());
        profissionalRepository.save(p);
        redirectAttributes.addFlashAttribute("msg", "Profissional atualizado com sucesso.");
        return "redirect:/admin/profissionais";
    }

    @PostMapping("/profissionais/{id}/excluir")
    public String excluirProfissional(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Profissional p = profissionalRepository.findById(id).orElseThrow();
            Long uid = p.getUsuario().getId();
            chamadoRepository.findAll().stream()
                .filter(c -> c.getProfissional() != null && id.equals(c.getProfissional().getId()))
                .forEach(c -> { c.setProfissional(null); chamadoRepository.save(c); });
            avaliacaoRepository.findAll().stream()
                .filter(a -> id.equals(a.getProfissional().getId()))
                .forEach(avaliacaoRepository::delete);
            profissionalRepository.delete(p);
            usuarioRepository.deleteById(uid);
            log.info("Profissional id={} excluído pelo admin.", id);
            redirectAttributes.addFlashAttribute("msg", "Profissional excluído com sucesso.");
        } catch (Exception e) {
            log.error("Erro ao excluir profissional id={}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("erro", "Não foi possível excluir o profissional.");
        }
        return "redirect:/admin/profissionais";
    }

    @GetMapping("/categorias")
    public String categorias(@RequestParam(required = false) String termo, Model model) {
        java.util.List<Categoria> todasCategorias = categoriaRepository.findAll();
        java.util.List<Categoria> categorias = todasCategorias;
        if (termo != null && !termo.isBlank()) {
            String t = termo.toLowerCase();
            categorias = todasCategorias.stream()
                .filter(c -> c.getNome().toLowerCase().contains(t)
                          || (c.getDescricao() != null && c.getDescricao().toLowerCase().contains(t)))
                .toList();
        }

        long totalAtivas = todasCategorias.stream().filter(Categoria::isAtiva).count();

        java.util.List<Profissional> todosProfissionais = profissionalRepository.findAll();
        java.util.Map<Long, Long> profissionaisPorCategoria = todosProfissionais.stream()
            .filter(p -> p.getCategoria() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                p -> p.getCategoria().getId(), java.util.stream.Collectors.counting()));
        long totalProfissionaisCadastrados = todosProfissionais.stream()
            .filter(p -> p.getCategoria() != null).count();

        java.util.Map<Long, Long> chamadosPorCategoriaMap = chamadoRepository.findAll().stream()
            .filter(c -> c.getCategoria() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                c -> c.getCategoria().getId(), java.util.stream.Collectors.counting()));

        model.addAttribute("categorias", categorias);
        model.addAttribute("totalCategorias", todasCategorias.size());
        model.addAttribute("totalAtivas", totalAtivas);
        model.addAttribute("totalProfissionaisCadastrados", totalProfissionaisCadastrados);
        model.addAttribute("profissionaisPorCategoria", profissionaisPorCategoria);
        model.addAttribute("chamadosPorCategoriaMap", chamadosPorCategoriaMap);
        model.addAttribute("termo", termo);
        model.addAttribute("currentPage", "categorias");
        return "admin/categorias";
    }

    @PostMapping("/categorias/nova")
    public String novaCategoria(@RequestParam String nome,
                                 @RequestParam(required = false) String icone,
                                 @RequestParam(required = false) String descricao,
                                 RedirectAttributes redirectAttributes) {
        if (categoriaRepository.existsByNomeIgnoreCase(nome)) {
            redirectAttributes.addFlashAttribute("erro", "Categoria ja existe.");
            return "redirect:/admin/categorias";
        }
        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        categoria.setIcone(icone != null && !icone.isBlank() ? icone : "bi-tools");
        categoria.setDescricao(descricao != null && !descricao.isBlank() ? descricao : null);
        categoriaRepository.save(categoria);
        redirectAttributes.addFlashAttribute("msg", "Categoria criada com sucesso!");
        return "redirect:/admin/categorias";
    }

    @PostMapping("/categorias/{id}/editar")
    public String editarCategoria(@PathVariable Long id,
                                   @RequestParam String nome,
                                   @RequestParam(required = false) String icone,
                                   @RequestParam(required = false) String descricao,
                                   RedirectAttributes redirectAttributes) {
        Categoria categoria = categoriaRepository.findById(id).orElseThrow();
        categoria.setNome(nome);
        if (icone != null && !icone.isBlank()) categoria.setIcone(icone);
        categoria.setDescricao(descricao != null && !descricao.isBlank() ? descricao : null);
        categoriaRepository.save(categoria);
        redirectAttributes.addFlashAttribute("msg", "Categoria atualizada com sucesso!");
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

    @PostMapping("/categorias/{id}/excluir")
    public String excluirCategoria(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Categoria categoria = categoriaRepository.findById(id).orElseThrow();
        profissionalRepository.findAll().stream()
            .filter(p -> p.getCategoria() != null && p.getCategoria().getId().equals(id))
            .forEach(p -> { p.setCategoria(null); profissionalRepository.save(p); });
        chamadoRepository.findAll().stream()
            .filter(c -> c.getCategoria() != null && c.getCategoria().getId().equals(id))
            .forEach(c -> { c.setCategoria(null); chamadoRepository.save(c); });
        categoriaRepository.delete(categoria);
        log.info("Categoria id={} '{}' excluída pelo admin.", id, categoria.getNome());
        redirectAttributes.addFlashAttribute("msg", "Categoria excluída.");
        return "redirect:/admin/categorias";
    }

    @GetMapping("/tarifas")
    public String tarifas(Model model) {
        model.addAttribute("tarifas", tarifaRepository.findAll());
        model.addAttribute("currentPage", "tarifas");
        return "admin/tarifas";
    }

    @PostMapping("/tarifas/nova")
    public String novaTarifa(@RequestParam String nome,
                             @RequestParam String tipo,
                             @RequestParam java.math.BigDecimal valor,
                             @RequestParam(required = false) String aplicacao,
                             @RequestParam(required = false) String descricao,
                             RedirectAttributes redirectAttributes) {
        com.servicematch.model.Tarifa t = new com.servicematch.model.Tarifa();
        t.setNome(nome);
        t.setTipo(tipo);
        t.setValor(valor);
        t.setAplicacao(aplicacao);
        t.setDescricao(descricao);
        tarifaRepository.save(t);
        redirectAttributes.addFlashAttribute("msg", "Tarifa criada com sucesso!");
        return "redirect:/admin/tarifas";
    }

    @PostMapping("/tarifas/{id}/toggle")
    public String toggleTarifa(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        com.servicematch.model.Tarifa t = tarifaRepository.findById(id).orElseThrow();
        t.setAtiva(!t.isAtiva());
        tarifaRepository.save(t);
        redirectAttributes.addFlashAttribute("msg", t.isAtiva() ? "Tarifa ativada." : "Tarifa desativada.");
        return "redirect:/admin/tarifas";
    }

    @PostMapping("/tarifas/{id}/excluir")
    public String excluirTarifa(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        tarifaRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("msg", "Tarifa excluída.");
        return "redirect:/admin/tarifas";
    }

    @GetMapping("/relatorios")
    public String relatorios(Model model) {
        long totalClientes      = usuarioRepository.findByTipo(TipoUsuario.CLIENTE).size();
        long totalProfissionais = profissionalRepository.count();
        long totalChamados      = chamadoRepository.count();
        long concluidos         = chamadoRepository.countByStatus(StatusChamado.CONCLUIDO);
        long abertos            = chamadoRepository.countByStatus(StatusChamado.ABERTO);
        long emAndamento        = chamadoRepository.countByStatus(StatusChamado.EM_ANDAMENTO);
        long cancelados         = chamadoRepository.countByStatus(StatusChamado.CANCELADO);
        long totalAvaliacoes    = avaliacaoRepository.count();

        double taxaConclusao = totalChamados > 0
                ? Math.round((concluidos * 100.0 / totalChamados) * 10.0) / 10.0 : 0;

        double mediaGeral = profissionalRepository.findAll().stream()
                .filter(p -> p.getAvaliacaoMedia() != null && p.getQuantidadeAvaliacoes() > 0)
                .mapToDouble(p -> p.getAvaliacaoMedia().doubleValue())
                .average().orElse(0.0);
        java.math.BigDecimal satisfacaoMedia = new java.math.BigDecimal(mediaGeral)
                .setScale(1, java.math.RoundingMode.HALF_UP);

        java.util.List<com.servicematch.model.Categoria> categorias = categoriaRepository.findAll();
        java.util.Map<String, Long> chamadosPorCategoria = new java.util.LinkedHashMap<>();
        for (com.servicematch.model.Categoria cat : categorias) {
            long count = chamadoRepository.findAll().stream()
                    .filter(c -> c.getCategoria() != null && cat.getId().equals(c.getCategoria().getId()))
                    .count();
            if (count > 0) chamadosPorCategoria.put(cat.getNome(), count);
        }

        model.addAttribute("totalClientes", totalClientes);
        model.addAttribute("totalProfissionais", totalProfissionais);
        model.addAttribute("totalChamados", totalChamados);
        model.addAttribute("concluidos", concluidos);
        model.addAttribute("abertos", abertos);
        model.addAttribute("emAndamento", emAndamento);
        model.addAttribute("cancelados", cancelados);
        model.addAttribute("totalAvaliacoes", totalAvaliacoes);
        model.addAttribute("taxaConclusao", taxaConclusao);
        model.addAttribute("satisfacaoMedia", satisfacaoMedia);
        long maxChamadosCategoria = chamadosPorCategoria.values().stream()
                .max(Long::compareTo).orElse(1L);

        model.addAttribute("chamadosPorCategoria", chamadosPorCategoria);
        model.addAttribute("maxChamadosCategoria", maxChamadosCategoria);
        model.addAttribute("currentPage", "relatorios");
        return "admin/relatorios";
    }

    @GetMapping("/configuracoes")
    public String configuracoes(Model model) {
        model.addAttribute("currentPage", "configuracoes");
        return "admin/configuracoes";
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
