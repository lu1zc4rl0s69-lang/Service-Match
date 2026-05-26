package com.servicematch.controller;

import com.servicematch.model.*;
import com.servicematch.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final CategoriaRepository categoriaRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String erro,
                        @RequestParam(required = false) String logout,
                        Model model) {
        if (erro != null) model.addAttribute("erro", "E-mail ou senha inválidos.");
        if (logout != null) model.addAttribute("msg", "Você saiu com sucesso.");
        return "auth/login";
    }

    @GetMapping("/cadastro")
    public String cadastro(Model model) {
        model.addAttribute("categorias", categoriaRepository.findByAtivaTrue());
        return "auth/cadastro";
    }

    @PostMapping("/cadastro")
    public String processarCadastro(
            @RequestParam String nome,
            @RequestParam String email,
            @RequestParam String telefone,
            @RequestParam String senha,
            @RequestParam String tipo,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) BigDecimal precoBase,
            @RequestParam(required = false) String descricao,
            RedirectAttributes redirectAttributes) {

        if (usuarioRepository.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("erro", "Este e-mail já está cadastrado.");
            return "redirect:/cadastro";
        }

        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setTelefone(telefone);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setTipo(TipoUsuario.valueOf(tipo.toUpperCase()));
        usuarioRepository.save(usuario);

        if ("PROFISSIONAL".equalsIgnoreCase(tipo)) {
            Profissional profissional = new Profissional();
            profissional.setUsuario(usuario);
            if (categoriaId != null) {
                categoriaRepository.findById(categoriaId).ifPresent(profissional::setCategoria);
            }
            profissional.setCidade(cidade);
            profissional.setEstado(estado);
            profissional.setPrecoBase(precoBase != null ? precoBase : BigDecimal.ZERO);
            profissional.setDescricao(descricao);
            profissional.setTokens(0);
            profissionalRepository.save(profissional);
        }

        redirectAttributes.addFlashAttribute("msg", "Conta criada com sucesso! Faça login.");
        return "redirect:/login";
    }
}
