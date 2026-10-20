package com.hardwarestore.venda.service;

import com.hardwarestore.cliente.model.Cliente;
import com.hardwarestore.cliente.repository.ClienteRepository;
import com.hardwarestore.common.exception.RecursoNaoEncontradoException;
import com.hardwarestore.common.exception.RegraDeNegocioException;
import com.hardwarestore.venda.dto.CarrinhoResponseDTO;
import com.hardwarestore.venda.model.Carrinho;
import com.hardwarestore.venda.model.ItemCarrinho;
import com.hardwarestore.venda.model.ItemCarrinhoExpirado;
import com.hardwarestore.venda.model.Produto;
import com.hardwarestore.venda.repository.CarrinhoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * RF0031 - Gerenciar carrinho de compra. RF0032 - Definir quantidade de
 * itens no carrinho, tanto na adicao quanto na visualizacao.
 *
 * RN0031 - "nao deve ser possivel adicionar um item sem estoque
 * disponivel, nem em quantidade superior a disponivel": a validacao
 * compara o que o cliente ja tem deste produto no carrinho, mais o que
 * esta pedindo agora, contra o estoque DISPONIVEL - ou seja, o estoque
 * menos o que outros clientes estao com bloqueado nos carrinhos deles
 * (RN0044).
 *
 * RN0044 - ao incluir um item, os itens do carrinho ficam bloqueados para
 * outros clientes por um prazo parametrizavel (app.carrinho.bloqueio-minutos)
 * contado a partir do ULTIMO item incluido; o cliente e avisado quando
 * faltam app.carrinho.aviso-minutos (ver CarrinhoResponseDTO.avisoExpiracao).
 * RN0045/RNF0042 - vencido o prazo, os itens saem do carrinho e continuam
 * listados como "expirados" ate o cliente adiciona-los de novo (ver
 * CarrinhoExpiracaoService).
 *
 * RN0032 - na finalizacao (ver revalidarParaFinalizacao) o estoque e
 * conferido de novo: a quantidade e ajustada automaticamente e itens que
 * ficaram indisponiveis saem do carrinho, com alerta ao cliente.
 */
@Service
public class CarrinhoService {

    private final CarrinhoRepository carrinhoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoService produtoService;
    private final CarrinhoExpiracaoService expiracaoService;
    private final int bloqueioMinutos;
    private final int avisoMinutos;

    public CarrinhoService(CarrinhoRepository carrinhoRepository,
                            ClienteRepository clienteRepository,
                            ProdutoService produtoService,
                            CarrinhoExpiracaoService expiracaoService,
                            @Value("${app.carrinho.bloqueio-minutos:30}") int bloqueioMinutos,
                            @Value("${app.carrinho.aviso-minutos:5}") int avisoMinutos) {
        this.carrinhoRepository = carrinhoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoService = produtoService;
        this.expiracaoService = expiracaoService;
        this.bloqueioMinutos = bloqueioMinutos;
        this.avisoMinutos = avisoMinutos;
    }

    public CarrinhoResponseDTO obterCarrinho(Long clienteId) {
        // RN0044/RN0045 - antes de mostrar, libera o que ja venceu, sem
        // esperar o job agendado.
        carrinhoRepository.findByClienteId(clienteId)
                .filter(c -> expiracaoService.temItemVencido(c, LocalDateTime.now()))
                .ifPresent(c -> expiracaoService.expirarVencidosDoCarrinho(c.getId()));
        return paraDTO(obterOuCriarCarrinho(clienteId));
    }

    @Transactional
    public CarrinhoResponseDTO adicionarItem(Long clienteId, Long produtoId, Integer quantidade) {
        validarQuantidadePositiva(quantidade);
        Carrinho carrinho = obterOuCriarCarrinho(clienteId);
        LocalDateTime agora = LocalDateTime.now();
        expiracaoService.expirarVencidos(carrinho, agora);
        Produto produto = produtoService.buscarEntidadePorId(produtoId);

        Optional<ItemCarrinho> existente = localizarItemPorProduto(carrinho, produtoId);
        int quantidadeJaNoCarrinho = existente.map(ItemCarrinho::getQuantidade).orElse(0);
        validarEstoqueDisponivel(produto, clienteId, quantidadeJaNoCarrinho, quantidade);

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
        // RNF0042 - adicionar de novo um produto que tinha expirado limpa o aviso dele.
        carrinho.getItensExpirados().removeIf(e -> e.getProduto().getId().equals(produtoId));
        renovarBloqueio(carrinho, agora);
        return paraDTO(carrinhoRepository.save(carrinho));
    }

    // RF0032 - alterar a quantidade de um item ja no carrinho (na propria
    // tela do carrinho, nao so no momento da adicao).
    @Transactional
    public CarrinhoResponseDTO alterarQuantidade(Long clienteId, Long itemId, Integer novaQuantidade) {
        validarQuantidadePositiva(novaQuantidade);
        Carrinho carrinho = buscarCarrinhoExistente(clienteId);
        LocalDateTime agora = LocalDateTime.now();
        expiracaoService.expirarVencidos(carrinho, agora);
        ItemCarrinho item = localizarItemPorId(carrinho, itemId);

        // valida contra o estoque disponivel, desconsiderando a quantidade
        // atual deste mesmo item (que esta sendo substituida, nao somada).
        validarEstoqueDisponivel(item.getProduto(), clienteId, 0, novaQuantidade);
        item.setQuantidade(novaQuantidade);
        renovarBloqueio(carrinho, agora);
        return paraDTO(carrinhoRepository.save(carrinho));
    }

    @Transactional
    public CarrinhoResponseDTO removerItem(Long clienteId, Long itemId) {
        Carrinho carrinho = buscarCarrinhoExistente(clienteId);
        ItemCarrinho item = localizarItemPorId(carrinho, itemId);
        carrinho.getItens().remove(item);
        return paraDTO(carrinhoRepository.save(carrinho));
    }

    // RNF0042 - "os itens deverao ser adicionados novamente no carrinho":
    // devolve ao carrinho a quantidade que tinha expirado (sujeita a
    // RN0031, ou seja, so se ainda houver estoque disponivel).
    @Transactional
    public CarrinhoResponseDTO readicionarExpirado(Long clienteId, Long expiradoId) {
        Carrinho carrinho = buscarCarrinhoExistente(clienteId);
        ItemCarrinhoExpirado expirado = localizarExpirado(carrinho, expiradoId);
        return adicionarItem(clienteId, expirado.getProduto().getId(), expirado.getQuantidade());
    }

    // RNF0042 - o cliente tambem pode dispensar o aviso sem readicionar.
    @Transactional
    public CarrinhoResponseDTO dispensarExpirado(Long clienteId, Long expiradoId) {
        Carrinho carrinho = buscarCarrinhoExistente(clienteId);
        ItemCarrinhoExpirado expirado = localizarExpirado(carrinho, expiradoId);
        carrinho.getItensExpirados().remove(expirado);
        return paraDTO(carrinhoRepository.save(carrinho));
    }

    /**
     * RN0032 (e RN0044/RNF0042) - conferencia feita no inicio da
     * finalizacao da compra. Roda em transacao PROPRIA e independente
     * (REQUIRES_NEW) de proposito: quando ha alertas, a finalizacao e
     * recusada com erro, e o ajuste do carrinho precisa sobreviver a esse
     * rollback para o cliente ver o carrinho ja corrigido.
     *
     * @return alertas a exibir ao cliente; lista vazia = pode seguir.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<String> revalidarParaFinalizacao(Long clienteId) {
        Carrinho carrinho = buscarCarrinhoExistente(clienteId);
        LocalDateTime agora = LocalDateTime.now();
        List<String> alertas = new ArrayList<>();

        // RN0044/RN0045/RNF0042 - itens cujo bloqueio venceu saem do carrinho.
        List<String> expirados = expiracaoService.expirarVencidos(carrinho, agora);
        if (!expirados.isEmpty()) {
            alertas.add("O prazo de bloqueio expirou e estes itens foram retirados do carrinho: "
                    + String.join(", ", expirados) + ". Adicione-os novamente para comprar (RNF0042).");
        } else if (!carrinho.getItensExpirados().isEmpty()) {
            alertas.add("Há itens expirados no carrinho. Adicione-os novamente (ou dispense o aviso) antes de comprar (RNF0042).");
        }

        // RN0032 - estoque mudou entre a adicao ao carrinho e a finalizacao.
        for (ItemCarrinho item : new ArrayList<>(carrinho.getItens())) {
            int disponivel = produtoService.quantidadeDisponivelPara(item.getProduto(), clienteId);
            if (item.getQuantidade() > disponivel) {
                String nome = item.getProduto().getNome();
                if (disponivel <= 0) {
                    carrinho.getItens().remove(item);
                    alertas.add("O produto \"" + nome + "\" ficou indisponível e foi removido do carrinho (RN0032).");
                } else {
                    alertas.add("A quantidade de \"" + nome + "\" foi ajustada de " + item.getQuantidade()
                            + " para " + disponivel + " porque o estoque mudou (RN0032).");
                    item.setQuantidade(disponivel);
                }
            }
        }

        if (!alertas.isEmpty()) {
            carrinhoRepository.save(carrinho);
        }
        return alertas;
    }

    // Usado pelo PedidoService ao finalizar a compra: os itens ja viraram
    // ItemPedido, entao o carrinho e esvaziado (e o bloqueio some com eles).
    @Transactional
    public void esvaziarAposFinalizarCompra(Carrinho carrinho) {
        carrinho.getItens().clear();
        carrinhoRepository.save(carrinho);
    }

    /** Total de unidades no carrinho (base do calculo do frete, RF0034); 0 se o cliente ainda nao tem carrinho. */
    public int unidadesNoCarrinho(Long clienteId) {
        return carrinhoRepository.findByClienteId(clienteId)
                .map(c -> c.getItens().stream().mapToInt(ItemCarrinho::getQuantidade).sum())
                .orElse(0);
    }

    public Carrinho buscarCarrinhoExistente(Long clienteId) {
        return carrinhoRepository.findByClienteId(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Este cliente ainda não tem um carrinho."));
    }

    private CarrinhoResponseDTO paraDTO(Carrinho carrinho) {
        return CarrinhoResponseDTO.fromEntity(carrinho, LocalDateTime.now(), avisoMinutos);
    }

    // RN0044 - "o prazo parametrizado deve ser relativo ao ultimo item
    // incluido no carrinho": cada inclusao/alteracao renova o prazo de TODOS
    // os itens.
    private void renovarBloqueio(Carrinho carrinho, LocalDateTime agora) {
        LocalDateTime ate = agora.plusMinutes(bloqueioMinutos);
        carrinho.getItens().forEach(i -> i.setBloqueadoAte(ate));
    }

    private Carrinho obterOuCriarCarrinho(Long clienteId) {
        return carrinhoRepository.findByClienteId(clienteId)
                .orElseGet(() -> {
                    Cliente cliente = clienteRepository.findById(clienteId)
                            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado. ID: " + clienteId));
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
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado no carrinho. ID: " + itemId));
    }

    private ItemCarrinhoExpirado localizarExpirado(Carrinho carrinho, Long expiradoId) {
        return carrinho.getItensExpirados().stream()
                .filter(e -> e.getId().equals(expiradoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item expirado não encontrado. ID: " + expiradoId));
    }

    private void validarEstoqueDisponivel(Produto produto, Long clienteId, int quantidadeJaNoCarrinho, int quantidadeAdicionalOuNova) {
        int quantidadeFinal = quantidadeJaNoCarrinho + quantidadeAdicionalOuNova;
        int disponivel = produtoService.quantidadeDisponivelPara(produto, clienteId);
        if (quantidadeFinal > disponivel) {
            String detalhe = disponivel < produto.getQuantidadeEstoque()
                    ? " (parte do estoque está bloqueada em carrinhos de outros clientes ou em compras em processamento - RN0044)"
                    : "";
            throw new RegraDeNegocioException(
                    "Estoque insuficiente para \"" + produto.getNome() + "\". Disponível: "
                            + disponivel + "." + detalhe);
        }
    }

    private void validarQuantidadePositiva(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new RegraDeNegocioException("A quantidade deve ser maior que zero.");
        }
    }
}
