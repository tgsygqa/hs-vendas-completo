package com.hardwarestore.venda.service;

import com.hardwarestore.cliente.model.CartaoCredito;
import com.hardwarestore.cliente.model.Cliente;
import com.hardwarestore.cliente.model.Endereco;
import com.hardwarestore.cliente.repository.ClienteRepository;
import com.hardwarestore.common.exception.RecursoNaoEncontradoException;
import com.hardwarestore.common.exception.RegraDeNegocioException;
import com.hardwarestore.venda.dto.PagamentoCartaoRequestDTO;
import com.hardwarestore.venda.dto.PedidoResponseDTO;
import com.hardwarestore.venda.model.Carrinho;
import com.hardwarestore.venda.model.Cupom;
import com.hardwarestore.venda.model.ItemCarrinho;
import com.hardwarestore.venda.model.ItemPedido;
import com.hardwarestore.venda.model.PagamentoPedido;
import com.hardwarestore.venda.model.Pedido;
import com.hardwarestore.venda.model.StatusPedido;
import com.hardwarestore.venda.model.TipoCupom;
import com.hardwarestore.venda.repository.CupomRepository;
import com.hardwarestore.venda.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * P2 a P5 do CDU02 (RF0033 a RF0038). Por instrucao explicita do professor
 * para esta entrega (06/10, fluxo de sucesso), o pedido NASCE e PARA em
 * EM_PROCESSAMENTO: a validacao da forma de pagamento e as transicoes de
 * estado seguintes (RN0037, RN0038, RF0039, RF0040, baixa em estoque -
 * RF0053/RN0028) estao fora do escopo desta fase e ficam para a entrega de
 * 20/10, quando o caso de uso completo for apresentado.
 */
@Service
public class PedidoService {

    // RF0034 - frete fixo para o fluxo de sucesso. O DRS nao detalha o
    // calculo (por peso, distancia, etc.); um valor fixo e simples cumpre
    // o requisito sem inventar uma regra que o DRS nao definiu - e e
    // exatamente o criterio a explicar na apresentacao.
    private static final BigDecimal FRETE_PADRAO = new BigDecimal("24.90");

    // RN0034 - valor minimo por cartao quando a compra usa mais de um.
    private static final BigDecimal VALOR_MINIMO_POR_CARTAO = new BigDecimal("10.00");

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final CupomRepository cupomRepository;
    private final CarrinhoService carrinhoService;

    public PedidoService(PedidoRepository pedidoRepository,
                          ClienteRepository clienteRepository,
                          CupomRepository cupomRepository,
                          CarrinhoService carrinhoService) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.cupomRepository = cupomRepository;
        this.carrinhoService = carrinhoService;
    }

    /**
     * RF0033 a RF0038 - inicia a finalizacao a partir do carrinho, aplica
     * o endereco (RF0035) e a forma de pagamento (RF0036/RF0037)
     * escolhidos, calcula o frete (RF0034) e cria o pedido em
     * EM_PROCESSAMENTO (RF0038). Nao ha passo seguinte nesta entrega.
     */
    @Transactional
    public PedidoResponseDTO finalizarCompra(Long clienteId, Long enderecoId,
                                              List<PagamentoCartaoRequestDTO> cartoesRequest,
                                              List<Long> cupomIdsRequest) {
        Cliente cliente = buscarCliente(clienteId);
        Carrinho carrinho = carrinhoService.buscarCarrinhoExistente(clienteId);
        if (carrinho.getItens().isEmpty()) {
            throw new RegraDeNegocioException("O carrinho esta vazio.");
        }

        Endereco endereco = localizarEndereco(cliente, enderecoId);

        List<PagamentoCartaoRequestDTO> cartoesReq = cartoesRequest != null ? cartoesRequest : List.of();
        List<Long> cupomIds = cupomIdsRequest != null ? cupomIdsRequest : List.of();
        if (new HashSet<>(cupomIds).size() != cupomIds.size()) {
            throw new RegraDeNegocioException("Um mesmo cupom foi informado mais de uma vez.");
        }

        // Carrega e valida cada cartao e cada cupom antes de mexer em
        // qualquer coisa.
        List<CartaoCredito> cartoes = new ArrayList<>();
        for (PagamentoCartaoRequestDTO c : cartoesReq) {
            cartoes.add(localizarCartao(cliente, c.getCartaoId()));
        }
        List<Cupom> cupons = new ArrayList<>();
        for (Long cupomId : cupomIds) {
            cupons.add(localizarCupom(cliente, cupomId));
        }

        // RN0033 - no maximo um cupom promocional por compra.
        long qtdPromocionais = cupons.stream().filter(c -> c.getTipo() == TipoCupom.PROMOCIONAL).count();
        if (qtdPromocionais > 1) {
            throw new RegraDeNegocioException("So e permitido usar um cupom promocional por compra (RN0033).");
        }

        BigDecimal valorProdutos = carrinho.getItens().stream()
                .map(i -> i.getProduto().getPrecoVenda().multiply(BigDecimal.valueOf(i.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal valorTotal = valorProdutos.add(FRETE_PADRAO);

        BigDecimal somaCartoes = cartoesReq.stream().map(PagamentoCartaoRequestDTO::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal somaCupons = cupons.stream().map(Cupom::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // RN0034 - com mais de um cartao e SEM cupons combinados, cada
        // cartao precisa valer ao menos R$ 10,00. RN0035 - combinando
        // cupons e cartoes, essa exigencia e relaxada (e so nesse caso).
        if (cartoesReq.size() > 1 && cupons.isEmpty()) {
            for (PagamentoCartaoRequestDTO c : cartoesReq) {
                if (c.getValor().compareTo(VALOR_MINIMO_POR_CARTAO) < 0) {
                    throw new RegraDeNegocioException(
                            "Cada cartao precisa cobrir ao menos R$ 10,00 quando a compra usa mais de um cartao (RN0034).");
                }
            }
        }

        BigDecimal valorRestanteAposCupons = valorTotal.subtract(somaCupons);
        BigDecimal troco = BigDecimal.ZERO;

        if (valorRestanteAposCupons.compareTo(BigDecimal.ZERO) > 0) {
            // Os cartoes precisam cobrir exatamente o que os cupons nao
            // cobriram - nem a menos (compra incompleta), nem a mais
            // (nesse caso o cliente deveria ter usado menos no cartao).
            if (somaCartoes.compareTo(valorRestanteAposCupons) != 0) {
                throw new RegraDeNegocioException(
                        "A soma dos cartoes e cupons precisa cobrir exatamente o valor da compra (R$ "
                                + valorTotal + "). Falta ou sobra: R$ "
                                + valorRestanteAposCupons.subtract(somaCartoes).abs() + ".");
            }
        } else {
            // Os cupons sozinhos ja cobrem o valor total - nenhum cartao
            // deveria ter sido usado.
            if (somaCartoes.compareTo(BigDecimal.ZERO) > 0) {
                throw new RegraDeNegocioException(
                        "Os cupons selecionados ja cobrem o valor da compra; nao use cartao de credito.");
            }
            // RN0036 - "sem permitir o uso de cupons desnecessarios": se
            // tirar o cupom de menor valor da lista os demais ainda
            // cobrem a compra, um cupom a mais foi selecionado.
            if (cupons.size() > 1) {
                BigDecimal menorCupom = cupons.stream().map(Cupom::getValor)
                        .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                BigDecimal somaSemOMenor = somaCupons.subtract(menorCupom);
                if (somaSemOMenor.compareTo(valorTotal) >= 0) {
                    throw new RegraDeNegocioException(
                            "Ha cupons desnecessarios selecionados: o valor da compra ja seria coberto sem o cupom de menor valor (RN0036).");
                }
            }
            troco = somaCupons.subtract(valorTotal);
        }

        // A partir daqui, tudo validado - monta o pedido.
        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .status(StatusPedido.EM_PROCESSAMENTO)
                .enderecoEntrega(endereco)
                .frete(FRETE_PADRAO)
                .valorTotal(valorTotal)
                .dataCriacao(LocalDateTime.now())
                .build();

        for (ItemCarrinho item : carrinho.getItens()) {
            pedido.getItens().add(ItemPedido.builder()
                    .pedido(pedido)
                    .produto(item.getProduto())
                    .quantidade(item.getQuantidade())
                    .precoUnitario(item.getProduto().getPrecoVenda())
                    .build());
        }

        for (int i = 0; i < cartoes.size(); i++) {
            pedido.getPagamentos().add(PagamentoPedido.builder()
                    .pedido(pedido)
                    .cartao(cartoes.get(i))
                    .valor(cartoesReq.get(i).getValor())
                    .build());
        }

        for (Cupom cupom : cupons) {
            cupom.setUtilizado(true);
            cupom.setPedidoUtilizacao(pedido);
            cupomRepository.save(cupom);
        }

        // RN0036 - gera o cupom de troco pela diferenca, se houver.
        if (troco.compareTo(BigDecimal.ZERO) > 0) {
            Cupom cupomTroco = Cupom.builder()
                    .codigo("TROCO-" + System.currentTimeMillis())
                    .tipo(TipoCupom.TROCA)
                    .valor(troco)
                    .utilizado(false)
                    .cliente(cliente)
                    .pedidoUtilizacao(pedido)
                    .build();
            cupomRepository.save(cupomTroco);
        }

        carrinhoService.esvaziarAposFinalizarCompra(carrinho);

        return PedidoResponseDTO.fromEntity(pedidoRepository.save(pedido));
    }

    public List<PedidoResponseDTO> listarTodos() {
        return pedidoRepository.findAll().stream()
                .map(PedidoResponseDTO::fromEntity)
                .toList();
    }

    public PedidoResponseDTO buscarPorIdDTO(Long pedidoId) {
        return PedidoResponseDTO.fromEntity(buscarPedido(pedidoId));
    }

    private Pedido buscarPedido(Long pedidoId) {
        return pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido nao encontrado. ID: " + pedidoId));
    }

    private Cliente buscarCliente(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado. ID: " + clienteId));
    }

    private Endereco localizarEndereco(Cliente cliente, Long enderecoId) {
        return cliente.getEnderecos().stream()
                .filter(e -> e.getId().equals(enderecoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereco nao encontrado. ID: " + enderecoId));
    }

    private CartaoCredito localizarCartao(Cliente cliente, Long cartaoId) {
        return cliente.getCartoes().stream()
                .filter(c -> c.getId().equals(cartaoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cartao nao encontrado. ID: " + cartaoId));
    }

    private Cupom localizarCupom(Cliente cliente, Long cupomId) {
        Cupom cupom = cupomRepository.findById(cupomId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cupom nao encontrado. ID: " + cupomId));
        if (cupom.isUtilizado()) {
            throw new RegraDeNegocioException("O cupom \"" + cupom.getCodigo() + "\" ja foi utilizado.");
        }
        boolean podeUsar = cupom.getCliente() == null || cupom.getCliente().getId().equals(cliente.getId());
        if (!podeUsar) {
            throw new RegraDeNegocioException("O cupom \"" + cupom.getCodigo() + "\" nao pertence a este cliente.");
        }
        return cupom;
    }
}
