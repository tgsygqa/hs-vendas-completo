package com.hardwarestore.venda.service;

import com.hardwarestore.common.exception.RecursoNaoEncontradoException;
import com.hardwarestore.venda.dto.ProdutoResponseDTO;
import com.hardwarestore.venda.model.Produto;
import com.hardwarestore.venda.repository.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * So consulta. O cadastro/manutencao completa de produto (RF0011-RF0016,
 * "Cadastro de Livros" no DRS) e um caso de uso separado, fora de escopo
 * aqui - os produtos existem porque o ProdutoSeeder os semeia.
 */
@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    public List<ProdutoResponseDTO> listarDisponiveis() {
        return produtoRepository.findAll().stream()
                .filter(Produto::isAtivo)
                .map(ProdutoResponseDTO::fromEntity)
                .toList();
    }

    // Usado internamente pelo CarrinhoService - devolve a entidade, nao o
    // DTO, porque quem chama precisa ler quantidadeEstoque para validar
    // RN0031 (e, futuramente, alterar o estoque quando RF0053 entrar em
    // escopo, na entrega de 20/10).
    Produto buscarEntidadePorId(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto nao encontrado. ID: " + id));
    }
}
