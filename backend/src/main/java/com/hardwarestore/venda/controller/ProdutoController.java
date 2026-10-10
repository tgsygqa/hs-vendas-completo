package com.hardwarestore.venda.controller;

import com.hardwarestore.venda.dto.ProdutoResponseDTO;
import com.hardwarestore.venda.service.ProdutoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Apenas leitura - o cadastro completo de produto (RF0011-RF0016) e um
 * caso de uso separado, fora de escopo aqui (ver ProdutoService). Esta
 * rota existe so para a tela de carrinho ter o que listar.
 */
@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public ResponseEntity<List<ProdutoResponseDTO>> listar() {
        return ResponseEntity.ok(produtoService.listarDisponiveis());
    }
}
