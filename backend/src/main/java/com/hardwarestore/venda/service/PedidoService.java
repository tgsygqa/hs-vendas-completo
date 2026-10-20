package com.hardwarestore.venda.service;

import com.hardwarestore.cliente.model.CartaoCredito;
import com.hardwarestore.cliente.model.Cliente;
import com.hardwarestore.cliente.model.Endereco;
import com.hardwarestore.cliente.repository.ClienteRepository;
import com.hardwarestore.common.exception.RecursoNaoEncontradoException;
import com.hardwarestore.common.exception.RegraDeNegocioException;
import com.hardwarestore.venda.dto.ConfirmarRecebimentoTrocaRequestDTO;
import com.hardwarestore.venda.dto.FreteResponseDTO;
import com.hardwarestore.venda.dto.ItemTrocaRequestDTO;
import com.hardwarestore.venda.dto.PagamentoCartaoRequestDTO;
import com.hardwarestore.venda.dto.PedidoResponseDTO;
import com.hardwarestore.venda.dto.SolicitacaoTrocaResponseDTO;
import com.hardwarestore.venda.dto.SolicitarTrocaRequestDTO;
import com.hardwarestore.venda.model.Carrinho;
import com.hardwarestore.venda.model.Cupom;
import com.hardwarestore.venda.model.ItemCarrinho;
import com.hardwarestore.venda.model.ItemPedido;
import com.hardwarestore.venda.model.ItemSolicitacaoTroca;
import com.hardwarestore.venda.model.PagamentoPedido;
import com.hardwarestore.venda.model.Pedido;
import com.hardwarestore.venda.model.Produto;
import com.hardwarestore.venda.model.SolicitacaoTroca;
import com.hardwarestore.venda.model.StatusPedido;
import com.hardwarestore.venda.model.TipoCupom;
import com.hardwarestore.venda.repository.CupomRepository;
import com.hardwarestore.venda.repository.PedidoRepository;
import com.hardwarestore.venda.repository.ProdutoRepository;
import com.hardwarestore.venda.repository.SolicitacaoTrocaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * CDU02 completo (RF0033 a RF0045): nasce em EM_PROCESSAMENTO (P2 a P5) e
 * percorre a validacao de pagamento (P6, RN0037/RN0038 - AUTOMATICA, feita
 * pelo sistema; ver PagamentoValidacaoJob), despacho e entrega (P7/P8,
 * RF0039/RF0040, pelo administrador) e, a partir de ENTREGUE, o processo de
 * troca inteiro (P9 a P11, RF0041-RF0045) - ver StatusPedido para o diagrama
 * de estados e SolicitacaoTroca para os detalhes de cada troca.
 */
@Service
public class PedidoService {

    // RN0034 - valor minimo por cartao quando a compra usa mais de um.
    private static final BigDecimal VALOR_MINIMO_POR_CARTAO = new BigDecimal("10.00");

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final CupomRepository cupomRepository;
    private final ProdutoRepository produtoRepository;
    private final SolicitacaoTrocaRepository solicitacaoTrocaRepository;
    private final CarrinhoService carrinhoService;
    private final FreteService freteService;
    private final PagamentoGatewayService pagamentoGatewayService;
    private final NotificacaoService notificacaoService;

    public PedidoService(PedidoRepository pedidoRepository,
                          ClienteRepository clienteRepository,
                          CupomRepository cupomRepository,
                          ProdutoRepository produtoRepository,
                          SolicitacaoTrocaRepository solicitacaoTrocaRepository,
                          CarrinhoService carrinhoService,
                          FreteService freteService,
                          PagamentoGatewayService pagamentoGatewayService,
                          NotificacaoService notificacaoService) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.cupomRepository = cupomRepository;
        this.produtoRepository = produtoRepository;
        this.solicitacaoTrocaRepository = solicitacaoTrocaRepository;
        this.carrinhoService = carrinhoService;
        this.freteService = freteService;
        this.pagamentoGatewayService = pagamentoGatewayService;
        this.notificacaoService = notificacaoService;
    }

    /**
     * RF0033 a RF0038 - inicia a finalizacao a partir do carrinho, aplica
     * o endereco (RF0035) e a forma de pagamento (RF0036/RF0037)
     * escolhidos, calcula o frete (RF0034) e cria o pedido em
     * EM_PROCESSAMENTO (RF0038). O passo seguinte (validacao do pagamento) e
     * automatico: ver PagamentoValidacaoJob / validarPagamento.
     */
    @Transactional
    public PedidoResponseDTO finalizarCompra(Long clienteId, Long enderecoId,
                                              List<PagamentoCartaoRequestDTO> cartoesRequest,
                                              List<Long> cupomIdsRequest) {
        Cliente cliente = buscarCliente(clienteId);

        // RN0032/RN0044/RNF0042 - antes de tudo, reconfere o carrinho: o item
        // pode ter ficado la um tempo, o estoque pode ter mudado (outro
        // cliente comprou o mesmo produto) ou o bloqueio pode ter expirado.
        // O carrinho e corrigido automaticamente (numa transacao propria, que
        // sobrevive ao erro abaixo) e o cliente recebe o alerta.
        List<String> alertas = carrinhoService.revalidarParaFinalizacao(clienteId);
        if (!alertas.isEmpty()) {
            throw new RegraDeNegocioException(String.join(" ", alertas));
        }

        Carrinho carrinho = carrinhoService.buscarCarrinhoExistente(clienteId);
        if (carrinho.getItens().isEmpty()) {
            throw new RegraDeNegocioException("O carrinho está vazio.");
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
            throw new RegraDeNegocioException("Só é permitido usar um cupom promocional por compra (RN0033).");
        }

        BigDecimal valorProdutos = carrinho.getItens().stream()
                .map(i -> i.getProduto().getPrecoVenda().multiply(BigDecimal.valueOf(i.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // RF0034 - frete pelas unidades do carrinho e pela UF do endereco escolhido.
        int unidades = carrinho.getItens().stream().mapToInt(ItemCarrinho::getQuantidade).sum();
        BigDecimal frete = freteService.calcular(endereco, unidades).valor();
        BigDecimal valorTotal = valorProdutos.add(frete);

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
                            "Cada cartão precisa cobrir ao menos R$ 10,00 quando a compra usa mais de um cartão (RN0034).");
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
                        "A soma dos cartões e cupons precisa cobrir exatamente o valor da compra ("
                                + brl(valorTotal) + "). Falta ou sobra: "
                                + brl(valorRestanteAposCupons.subtract(somaCartoes).abs()) + ".");
            }
        } else {
            // Os cupons sozinhos ja cobrem o valor total - nenhum cartao
            // deveria ter sido usado.
            if (somaCartoes.compareTo(BigDecimal.ZERO) > 0) {
                throw new RegraDeNegocioException(
                        "Os cupons selecionados já cobrem o valor da compra; não use cartão de crédito.");
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
                            "Há cupons desnecessários selecionados: o valor da compra já seria coberto sem o cupom de menor valor (RN0036).");
                }
            }
            troco = somaCupons.subtract(valorTotal);
        }

        // A partir daqui, tudo validado - monta o pedido.
        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .status(StatusPedido.EM_PROCESSAMENTO)
                .enderecoEntrega(endereco)
                .frete(frete)
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

    /**
     * RF0034 - frete que sera cobrado se o cliente comprar o carrinho atual
     * com o endereco informado (a tela de compra chama isto ao escolher o endereco).
     */
    public FreteResponseDTO calcularFrete(Long clienteId, Long enderecoId) {
        Cliente cliente = buscarCliente(clienteId);
        return freteService.calcular(localizarEndereco(cliente, enderecoId), carrinhoService.unidadesNoCarrinho(clienteId));
    }

    // Painel do administrador: todos os pedidos (ou so os de um status), mais recentes primeiro.
    public List<PedidoResponseDTO> listarTodos(StatusPedido status) {
        List<Pedido> pedidos = status == null
                ? pedidoRepository.findAllByOrderByIdDesc()
                : pedidoRepository.findByStatusOrderByIdDesc(status);
        return pedidos.stream().map(PedidoResponseDTO::fromEntity).toList();
    }

    // "Meus pedidos" do cliente.
    public List<PedidoResponseDTO> listarDoCliente(Long clienteId) {
        buscarCliente(clienteId);
        return pedidoRepository.findByClienteIdOrderByIdDesc(clienteId).stream()
                .map(PedidoResponseDTO::fromEntity)
                .toList();
    }

    // RF0043 - o administrador visualiza todas as solicitacoes de troca.
    public List<SolicitacaoTrocaResponseDTO> listarTrocas() {
        return solicitacaoTrocaRepository.findAllByOrderByIdDesc().stream()
                .map(SolicitacaoTrocaResponseDTO::fromEntity)
                .toList();
    }

    public PedidoResponseDTO buscarPorIdDTO(Long pedidoId) {
        return PedidoResponseDTO.fromEntity(buscarPedido(pedidoId));
    }

    private Pedido buscarPedido(Long pedidoId) {
        return pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado. ID: " + pedidoId));
    }

    private Cliente buscarCliente(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado. ID: " + clienteId));
    }

    private Endereco localizarEndereco(Cliente cliente, Long enderecoId) {
        return cliente.getEnderecos().stream()
                .filter(e -> e.getId().equals(enderecoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereço não encontrado. ID: " + enderecoId));
    }

    private CartaoCredito localizarCartao(Cliente cliente, Long cartaoId) {
        return cliente.getCartoes().stream()
                .filter(c -> c.getId().equals(cartaoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cartão não encontrado. ID: " + cartaoId));
    }

    private Cupom localizarCupom(Cliente cliente, Long cupomId) {
        Cupom cupom = cupomRepository.findById(cupomId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cupom não encontrado. ID: " + cupomId));
        if (cupom.isUtilizado()) {
            throw new RegraDeNegocioException("O cupom \"" + cupom.getCodigo() + "\" já foi utilizado.");
        }
        boolean podeUsar = cupom.getCliente() == null || cupom.getCliente().getId().equals(cliente.getId());
        if (!podeUsar) {
            throw new RegraDeNegocioException("O cupom \"" + cupom.getCodigo() + "\" não pertence a este cliente.");
        }
        return cupom;
    }

    // ------------------------------------------------------------------
    // P6 - Validacao de pagamento (RN0037/RN0038)
    // ------------------------------------------------------------------

    /**
     * RN0037 - "validar a veracidade dos cupons e o aceite da compra pela
     * operadora do cartao". Os cupons ja foram validados na finalizacao
     * (RN0033/RN0034/RN0035/RN0036); o que falta aqui e consultar a
     * "operadora" (PagamentoGatewayService) pelo cartao. RN0038 - aprovada
     * -> baixa em estoque (RF0053) e status APROVADA; reprovada -> itens
     * desbloqueados e mantidos em estoque (RN0028) e status REPROVADA. So
     * pode ser chamado com o pedido em EM_PROCESSAMENTO. Quem chama e o
     * sistema (PagamentoValidacaoJob), nunca um usuario.
     */
    @Transactional
    public PedidoResponseDTO validarPagamento(Long pedidoId) {
        Pedido pedido = buscarPedido(pedidoId);
        exigirStatus(pedido, StatusPedido.EM_PROCESSAMENTO);

        PagamentoGatewayService.Autorizacao autorizacao = pagamentoGatewayService.autorizar(pedido);
        String motivoRecusa = autorizacao.aprovada() ? null : autorizacao.motivo();

        // Salvaguarda: entre a finalizacao e a aprovacao outro pedido pode
        // ter consumido o estoque. Aprovar mesmo assim deixaria o estoque
        // negativo, entao o pedido e reprovado.
        if (motivoRecusa == null) {
            for (ItemPedido item : pedido.getItens()) {
                if (item.getProduto().getQuantidadeEstoque() < item.getQuantidade()) {
                    motivoRecusa = "Estoque insuficiente para \"" + item.getProduto().getNome()
                            + "\" no momento da aprovação.";
                    break;
                }
            }
        }

        if (motivoRecusa == null) {
            // RF0053/RN0038 - baixa em estoque so acontece na aprovacao,
            // nunca antes (RN0028 continua garantido: se reprovar, nada
            // aqui foi tocado).
            for (ItemPedido item : pedido.getItens()) {
                Produto produto = item.getProduto();
                produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() - item.getQuantidade());
                produtoRepository.save(produto);
            }
            pedido.setStatus(StatusPedido.APROVADA);
        } else {
            pedido.setStatus(StatusPedido.REPROVADA);
            pedido.setMotivoReprovacao(motivoRecusa);
            // Assumido (o DRS nao diz): numa compra reprovada, os cupons usados
            // voltam a ficar disponiveis - senao o cliente perderia o cupom
            // por causa de um cartao recusado.
            for (Cupom cupom : new ArrayList<>(pedido.getCuponsUtilizados())) {
                if (cupom.isUtilizado()) {
                    cupom.setUtilizado(false);
                    cupom.setPedidoUtilizacao(null);
                    cupomRepository.save(cupom);
                    pedido.getCuponsUtilizados().remove(cupom);
                }
            }
        }

        return PedidoResponseDTO.fromEntity(pedidoRepository.save(pedido));
    }

    // ------------------------------------------------------------------
    // P7/P8 - Despacho e entrega (RF0039/RF0040)
    // ------------------------------------------------------------------

    /** RF0039/RN0039 - so despacha um pedido APROVADA. */
    @Transactional
    public PedidoResponseDTO despachar(Long pedidoId) {
        Pedido pedido = buscarPedido(pedidoId);
        exigirStatus(pedido, StatusPedido.APROVADA);
        pedido.setStatus(StatusPedido.EM_TRANSPORTE);
        return PedidoResponseDTO.fromEntity(pedidoRepository.save(pedido));
    }

    /** RF0040/RN0040 - so confirma entrega de um pedido EM_TRANSPORTE. */
    @Transactional
    public PedidoResponseDTO confirmarEntrega(Long pedidoId) {
        Pedido pedido = buscarPedido(pedidoId);
        exigirStatus(pedido, StatusPedido.EM_TRANSPORTE);
        pedido.setStatus(StatusPedido.ENTREGUE);
        return PedidoResponseDTO.fromEntity(pedidoRepository.save(pedido));
    }

    // ------------------------------------------------------------------
    // P9 a P11 - Processo de troca (RF0041 a RF0045)
    // ------------------------------------------------------------------

    /**
     * RF0041 - o cliente solicita a troca de um ou mais itens de uma
     * compra. RN0043 - so pode ser solicitada a partir de ENTREGUE.
     */
    @Transactional
    public SolicitacaoTrocaResponseDTO solicitarTroca(Long pedidoId, SolicitarTrocaRequestDTO request) {
        Pedido pedido = buscarPedido(pedidoId);
        exigirStatus(pedido, StatusPedido.ENTREGUE);

        SolicitacaoTroca solicitacao = SolicitacaoTroca.builder()
                .pedido(pedido)
                .motivoCliente(request.getMotivoCliente())
                .dataSolicitacao(LocalDateTime.now())
                .build();

        List<Long> itensSolicitados = request.getItens().stream()
                .map(ItemTrocaRequestDTO::getItemPedidoId).filter(Objects::nonNull).toList();
        if (new HashSet<>(itensSolicitados).size() != itensSolicitados.size()) {
            throw new RegraDeNegocioException("O mesmo item foi informado mais de uma vez na solicitação de troca.");
        }

        for (ItemTrocaRequestDTO itemReq : request.getItens()) {
            ItemPedido itemPedido = pedido.getItens().stream()
                    .filter(i -> i.getId().equals(itemReq.getItemPedidoId()))
                    .findFirst()
                    .orElseThrow(() -> new RegraDeNegocioException(
                            "O item " + itemReq.getItemPedidoId() + " não pertence a este pedido."));
            if (itemReq.getQuantidade() > itemPedido.getQuantidade()) {
                throw new RegraDeNegocioException(
                        "A quantidade a trocar do item \"" + itemPedido.getProduto().getNome()
                                + "\" é maior do que a quantidade comprada.");
            }
            solicitacao.getItens().add(ItemSolicitacaoTroca.builder()
                    .solicitacao(solicitacao)
                    .itemPedido(itemPedido)
                    .quantidade(itemReq.getQuantidade())
                    .build());
        }

        pedido.setStatus(StatusPedido.EM_TROCA);
        pedidoRepository.save(pedido);
        return SolicitacaoTrocaResponseDTO.fromEntity(solicitacaoTrocaRepository.save(solicitacao));
    }

    /** RF0042/RN0046 - o administrador autoriza a troca solicitada. */
    @Transactional
    public SolicitacaoTrocaResponseDTO autorizarTroca(Long solicitacaoId) {
        SolicitacaoTroca solicitacao = buscarSolicitacaoTroca(solicitacaoId);
        exigirStatus(solicitacao.getPedido(), StatusPedido.EM_TROCA);
        exigirSolicitacaoEmAberto(solicitacao);

        solicitacao.setDataDecisaoAdmin(LocalDateTime.now());
        solicitacao.getPedido().setStatus(StatusPedido.TROCA_AUTORIZADA);
        pedidoRepository.save(solicitacao.getPedido());
        // RN0046 - o cliente e notificado da autorizacao.
        notificacaoService.notificar(solicitacao.getPedido().getCliente(),
                "Sua solicitação de troca do pedido #" + solicitacao.getPedido().getId()
                        + " foi AUTORIZADA. Envie o(s) item(ns) para a loja; assim que recebermos, "
                        + "você recebe um cupom de troca.");
        return SolicitacaoTrocaResponseDTO.fromEntity(solicitacaoTrocaRepository.save(solicitacao));
    }

    /**
     * RF0044 - confirma o recebimento do item devolvido e registra se ele
     * retorna ao estoque. RF0054 - se sim, reentrada em estoque. RN0042 -
     * pedido vai para TROCADO. RF0045 - gera o cupom de troca, no valor
     * total dos itens trocados.
     */
    @Transactional
    public SolicitacaoTrocaResponseDTO confirmarRecebimentoTroca(Long solicitacaoId,
                                                                   ConfirmarRecebimentoTrocaRequestDTO request) {
        SolicitacaoTroca solicitacao = buscarSolicitacaoTroca(solicitacaoId);
        exigirStatus(solicitacao.getPedido(), StatusPedido.TROCA_AUTORIZADA);
        if (solicitacao.getDataDecisaoAdmin() == null || solicitacao.getDataRecebimento() != null) {
            throw new RegraDeNegocioException("Esta solicitação de troca não está aguardando recebimento.");
        }

        solicitacao.setItensRetornaramEstoque(request.getItensRetornaramEstoque());
        solicitacao.setDataRecebimento(LocalDateTime.now());

        BigDecimal valorCupom = BigDecimal.ZERO;
        for (ItemSolicitacaoTroca item : solicitacao.getItens()) {
            BigDecimal valorItem = item.getItemPedido().getPrecoUnitario()
                    .multiply(BigDecimal.valueOf(item.getQuantidade()));
            valorCupom = valorCupom.add(valorItem);

            // RF0054 - reentrada em estoque, so quando o administrador
            // confirma que o item voltou em condicoes de venda.
            if (Boolean.TRUE.equals(request.getItensRetornaramEstoque())) {
                Produto produto = item.getItemPedido().getProduto();
                produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() + item.getQuantidade());
                produtoRepository.save(produto);
            }
        }

        // RF0045 - cupom de troca, no valor da troca, para o cliente usar
        // numa compra futura (ver PedidoService.finalizarCompra/RN0033).
        Cupom cupomTroca = Cupom.builder()
                .codigo("TROCA-P" + solicitacao.getPedido().getId() + "-S" + solicitacao.getId())
                .tipo(TipoCupom.TROCA)
                .valor(valorCupom)
                .utilizado(false)
                .cliente(solicitacao.getPedido().getCliente())
                .build();
        cupomRepository.save(cupomTroca);
        solicitacao.setCupomGerado(cupomTroca);

        solicitacao.getPedido().setStatus(StatusPedido.TROCADO);
        pedidoRepository.save(solicitacao.getPedido());
        // RF0045 - o cupom e disponibilizado ao cliente (ver GET /api/clientes/{id}/cupons).
        notificacaoService.notificar(solicitacao.getPedido().getCliente(),
                "Recebemos os itens da troca do pedido #" + solicitacao.getPedido().getId()
                        + ". O cupom " + cupomTroca.getCodigo() + " no valor de " + brl(valorCupom)
                        + " já está disponível para a sua próxima compra.");
        return SolicitacaoTrocaResponseDTO.fromEntity(solicitacaoTrocaRepository.save(solicitacao));
    }

    public List<SolicitacaoTrocaResponseDTO> listarSolicitacoesTroca(Long pedidoId) {
        Pedido pedido = buscarPedido(pedidoId);
        return pedido.getSolicitacoesTroca().stream()
                .map(SolicitacaoTrocaResponseDTO::fromEntity)
                .toList();
    }

    private SolicitacaoTroca buscarSolicitacaoTroca(Long solicitacaoId) {
        return solicitacaoTrocaRepository.findById(solicitacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Solicitação de troca não encontrada. ID: " + solicitacaoId));
    }

    // Valor em reais no formato brasileiro (R$ 1.234,56) para as mensagens ao usuario.
    private static String brl(BigDecimal valor) {
        return String.format(new java.util.Locale("pt", "BR"), "R$ %,.2f", valor);
    }

    // Uma solicitacao so pode ser autorizada uma vez.
    private void exigirSolicitacaoEmAberto(SolicitacaoTroca solicitacao) {
        if (solicitacao.getDataDecisaoAdmin() != null) {
            throw new RegraDeNegocioException("Esta solicitação de troca já foi autorizada.");
        }
    }

    private void exigirStatus(Pedido pedido, StatusPedido esperado) {
        if (pedido.getStatus() != esperado) {
            throw new RegraDeNegocioException(
                    "O pedido " + pedido.getId() + " precisa estar em " + esperado
                            + " para esta operação (está em " + pedido.getStatus() + ").");
        }
    }
}