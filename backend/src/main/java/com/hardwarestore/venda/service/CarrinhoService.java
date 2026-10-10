package com.hardwarestore.venda.service;

import com.hardwarestore.cliente.model.Cliente;
import com.hardwarestore.cliente.repository.ClienteRepository;
import com.hardwarestore.common.exception.RecursoNaoEncontradoException;
import com.hardwarestore.common.exception.RegraDeNegocioException;
import com.hardwarestore.venda.dto.CarrinhoResponseDTO;
import com.hardwarestore.venda.model.Carrinho;
import com.hardwarestore.venda.model.ItemCarrinho;
import com.hardwarestore.venda.model.Produto;
import com.hardwarestore.venda.repository.CarrinhoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * RF0031 - Gerenciar carrinho de compra. RF0032 - Definir quantidade de
 * itens no carrinho, tanto na adicao quanto na visualizacao.
 *
 * RN0031 - "nao deve ser possivel adicionar um item sem estoque
 * disponivel, nem em quantidade superior a disponivel": a validacao
 * compara o que o cliente ja tem deste produto no carrinho, mais o que
 * esta pedindo agora, contra Produto.quantidadeEstoque. Nao ha reserva
 * nem bloqueio entre clientes aqui - RN0032 (revalidar na finalizacao),
 * RN0044 e RN0045 (bloqueio/expiracao) estao fora do escopo desta entrega
 * (instrucoes do professor para 06/10) e ficam para 20/10.
 */
@Service
public class CarrinhoService {

    private final CarrinhoRepository carrinhoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoService produtoService;

    public CarrinhoService(CarrinhoRepository carrinhoRepository,
                            ClienteRepository clienteRepository,
                            ProdutoService produtoService) {
        this.carrinhoRepository = carrinhoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoService = produtoService;
    }

    public CarrinhoResponseDTO obterCarrinho(Long clienteId) {
        return CarrinhoResponseDTO.fromEntity(obterOuCriarCarrinho(clienteId));
    }

    @Transactional
    public CarrinhoResponseDTO adicionarItem(Long clienteId, Long produtoId, Integer quantidade) {
        validarQuantidadePositiva(quantidade);
        Carrinho carrinho = obterOuCriarCarrinho(clienteId);
        Produto produto = produtoService.buscarEntidadePorId(produtoId);

        Optional<ItemCarrinho> existente = localizarItemPorProduto(carrinho, produtoId);
        int quantidadeJaNoCarrinho = existente.map(ItemCarrinho::getQuantidade).orElse(0);
        validarEstoqueDisponivel(produto, quantidadeJaNoCarrinho, quantidade);

        if (existente.isPresent()) {
            ItemCarrinho item = existente.get();
            item.setQuantidade(item.getQuantidade() + quantidade);
        } else {
            carrinho.getItens().add(ItemCarrinho.builder()
                    .carrinho(carrinho)
                    .produto(produto)
                    .quantidade(quantidade)
                    .build());
        }
        return CarrinhoResponseDTO.fromEntity(carrinhoRepository.save(carrinho));
    }

    // RF0032 - alterar a quantidade de um item ja no carrinho (na propria
    // tela do carrinho, nao so no momento da adicao).
    @Transactional
    public CarrinhoResponseDTO alterarQuantidade(Long clienteId, Long itemId, Integer novaQuantidade) {
        validarQuantidadePositiva(novaQuantidade);
        Carrinho carrinho = buscarCarrinhoExistente(clienteId);
        ItemCarrinho item = localizarItemPorId(carrinho, itemId);

        // valida contra o estoque total, desconsiderando a quantidade
        // atual deste mesmo item (que esta sendo substituida, nao somada).
        validarEstoqueDisponivel(item.getProduto(), 0, novaQuantidade);
        item.setQuantidade(novaQuantidade);
        return CarrinhoResponseDTO.fromEntity(carrinhoRepository.save(carrinho));
    }

    @Transactional
    public CarrinhoResponseDTO removerItem(Long clienteId, Long itemId) {
        Carrinho carrinho = buscarCarrinhoExistente(clienteId);
        ItemCarrinho item = localizarItemPorId(carrinho, itemId);
        carrinho.getItens().remove(item);
        return CarrinhoResponseDTO.fromEntity(carrinhoRepository.save(carrinho));
    }

    // Usado pelo PedidoService (mesmo pacote) ao finalizar a compra: os
    // itens ja viraram ItemPedido, entao o carrinho e esvaziado.
    @Transactional
    void esvaziarAposFinalizarCompra(Carrinho carrinho) {
        carrinho.getItens().clear();
        carrinhoRepository.save(carrinho);
    }

    Carrinho buscarCarrinhoExistente(Long clienteId) {
        return carrinhoRepository.findByClienteId(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Este cliente ainda nao tem um carrinho."));
    }

    private Carrinho obterOuCriarCarrinho(Long clienteId) {
        return carrinhoRepository.findByClienteId(clienteId)
                .orElseGet(() -> {
                    Cliente cliente = clienteRepository.findById(clienteId)
                            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado. ID: " + clienteId));
                    return carrinhoRepository.save(Carrinho.builder().cliente(cliente).build());
                });
    }

    private Optional<ItemCarrinho> localizarItemPorProduto(Carrinho carrinho, Long produtoId) {
        return carrinho.getItens().stream()
                .filter(i -> i.getProduto().getId().equals(produtoId))
                .findFirst();
    }

    private ItemCarrinho localizarItemPorId(Carrinho carrinho, Long itemId) {
        return carrinho.getItens().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item nao encontrado no carrinho. ID: " + itemId));
    }

    private void validarEstoqueDisponivel(Produto produto, int quantidadeJaNoCarrinho, int quantidadeAdicionalOuNova) {
        int quantidadeFinal = quantidadeJaNoCarrinho + quantidadeAdicionalOuNova;
        if (quantidadeFinal > produto.getQuantidadeEstoque()) {
            throw new RegraDeNegocioException(
                    "Estoque insuficiente para \"" + produto.getNome() + "\". Disponivel: "
                            + produto.getQuantidadeEstoque() + ".");
        }
    }

    private void validarQuantidadePositiva(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new RegraDeNegocioException("A quantidade deve ser maior que zero.");
        }
    }
}
