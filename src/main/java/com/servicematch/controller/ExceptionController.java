package com.servicematch.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.util.NoSuchElementException;

@ControllerAdvice
public class ExceptionController {

    private static final Logger log = LoggerFactory.getLogger(ExceptionController.class);

    @ExceptionHandler(NoSuchElementException.class)
    public ModelAndView naoEncontrado(NoSuchElementException ex) {
        log.warn("Recurso não encontrado: {}", ex.getMessage());
        ModelAndView model = new ModelAndView("error");
        model.addObject("status", 404);
        model.addObject("error", "Recurso não encontrado");
        model.addObject("message", "O recurso solicitado não existe ou foi removido.");
        return model;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView acessoNegado(AccessDeniedException ex) {
        log.warn("Acesso negado: {}", ex.getMessage());
        ModelAndView model = new ModelAndView("error");
        model.addObject("status", 403);
        model.addObject("error", "Acesso negado");
        model.addObject("message", "Você não tem permissão para acessar este recurso.");
        return model;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView violacaoIntegridade(DataIntegrityViolationException ex) {
        log.error("Violação de integridade de dados: {}", ex.getMessage(), ex);
        ModelAndView model = new ModelAndView("error");
        model.addObject("status", 500);
        model.addObject("error", "Erro de dados");
        model.addObject("message", "Violação na integração dos dados.");
        return model;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView erroGeral(Exception ex) {
        log.error("Erro inesperado: {}", ex.getMessage(), ex);
        ModelAndView model = new ModelAndView("error");
        model.addObject("status", 500);
        model.addObject("error", "Erro interno");
        model.addObject("message", "Ocorreu um erro inesperado. Tente novamente mais tarde.");
        return model;
    }
}
