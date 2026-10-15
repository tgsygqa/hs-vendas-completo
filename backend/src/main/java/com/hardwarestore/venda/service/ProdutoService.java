package com.hardwarestore.venda.service;

import com.hardwarestore.common.exception.RecursoNaoEncontradoException;
import com.hardwarestore.venda.dto.ProdutoResponseDTO;
import com.hardwarestore.venda.model.Produto;
import com.hardwarestore.venda.repository.ItemCarrinhoRepository;
import com.hardwarestore.venda.repository.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * So consulta. O cadastro/manutencao completa de produto (RF0011-RF0016,
 * "Cadastro de Livros" no DRS) e um caso de uso separado, fora de escopo
 * aqui - os produtos existem porque o ProdutoSeeder os semeia.
 */
@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final ItemCarrinhoRepository itemCarrinhoRepository;

    public ProdutoService(ProdutoRepository produtoRepository,
                          ItemCarrinhoRepository itemCarrinhoRepository) {
        this.produtoRepository = produtoRepository;
        this.itemCarrinhoRepository = itemCarrinhoRepository;
    }

    /**
     * @param clienteId quem esta olhando o catalogo (a reserva feita no
     *                  proprio carrinho dele nao conta contra ele); nulo =
     *                  considera a reserva de todos os carrinhos.
     */
    public List<ProdutoResponseDTO> listarDisponiveis(Long clienteId) {
        return produtoRepository.findAll().stream()
                .filter(Produto::isAtivo)
                .map(p -> ProdutoResponseDTO.fromEntity(p, quantidadeDisponivelPara(p, clienteId)))
                .toList();
    }

    /**
     * RN0031/RN0044 - estoque menos o que outros clientes bloquearam nos
     * carrinhos deles (com prazo ainda valido). Nunca negativo.
     */
    public int quantidadeDisponivelPara(Produto produto, Long clienteId) {
        long clienteConsiderado = clienteId == null ? -1L : clienteId;
        long reservado = itemCarrinhoRepository.somarReservadoPorOutros(
                produto.getId(), clienteConsiderado, LocalDateTime.now());
        return (int) Math.max(0, produto.getQuantidadeEstoque() - reservado);
    }

    // Usado internamente pelo CarrinhoService - devolve a entidade, nao o
    // DTO, porque quem chama precisa ler quantidadeEstoque para validar
    // RN0031.
    public Produto buscarEntidadePorId(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado. ID: " + id));
    }
}
