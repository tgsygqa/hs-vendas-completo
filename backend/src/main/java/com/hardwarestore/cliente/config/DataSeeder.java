package com.hardwarestore.cliente.config;

import com.hardwarestore.cliente.model.Bandeira;
import com.hardwarestore.cliente.model.CartaoCredito;
import com.hardwarestore.cliente.model.Cliente;
import com.hardwarestore.cliente.model.Endereco;
import com.hardwarestore.cliente.model.Genero;
import com.hardwarestore.cliente.model.Telefone;
import com.hardwarestore.cliente.model.TipoTelefone;
import com.hardwarestore.cliente.repository.ClienteRepository;
import com.hardwarestore.venda.model.ItemPedido;
import com.hardwarestore.venda.model.PagamentoPedido;
import com.hardwarestore.venda.model.Pedido;
import com.hardwarestore.venda.model.Produto;
import com.hardwarestore.venda.model.StatusPedido;
import com.hardwarestore.venda.repository.PedidoRepository;
import com.hardwarestore.venda.repository.ProdutoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Popula o banco (H2 em memoria por padrao; ver application.properties) com o mesmo cliente mock ("Joao Silva") usado no
 * front (V08.html), incluindo endereco, cartao e 2 pedidos de exemplo -
 * para que a demonstracao em sala parta de um estado realista. Roda uma vez
 * a cada subida da aplicacao.
 *
 * @Order(2) - roda depois do ProdutoSeeder (modulo Vendas), porque os
 * pedidos de exemplo abaixo referenciam produtos ja semeados.
 */
@Component
@Order(2)
public class DataSeeder implements CommandLineRunner {

    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;

    public DataSeeder(ClienteRepository clienteRepository,
                       ProdutoRepository produtoRepository,
                       PedidoRepository pedidoRepository) {
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Override
    public void run(String... args) {
        Cliente joao = Cliente.builder()
                .nome("João Silva")
                .cpf("123.456.789-00")
                .genero(Genero.M)
                .dataNascimento(LocalDate.of(1990, 5, 15))
                .telefone(new Telefone(TipoTelefone.CELULAR, "11", "98765-4321"))
                .email("joao.silva@email.com")
                .senha(new BCryptPasswordEncoder().encode("SenhaInicial@123"))
                .ativo(true)
                .ranking(85)
                .build();

        Endereco endereco = Endereco.builder()
                .apelido("Casa")
                .tipoResidencia("Casa")
                .tipoLogradouro("Rua")
                .logradouro("das Flores")
                .numero("123")
                .bairro("Centro")
                .cep("01000-000")
                .cidade("São Paulo")
                .estado("SP")
                .pais("Brasil")
                .cobranca(true)
                .entrega(true)
                .cliente(joao)
                .build();
        joao.getEnderecos().add(endereco);

        CartaoCredito cartao = CartaoCredito.builder()
                .bandeira(Bandeira.VISA)
                .ultimosQuatroDigitos("4242")
                .titular("JOAO SILVA")
                .preferencial(true)
                .cliente(joao)
                .build();
        joao.getCartoes().add(cartao);

        clienteRepository.save(joao);

        // Produtos ja semeados pelo ProdutoSeeder (roda antes, @Order(1)).
        // Busca pelo nome em vez de id fixo, para nao depender da ordem
        // exata de insercao.
        List<Produto> produtos = produtoRepository.findAll();
        Produto ssd = produtos.stream()
                .filter(p -> p.getNome().contains("SSD"))
                .findFirst().orElseThrow();
        Produto headset = produtos.stream()
                .filter(p -> p.getNome().contains("Headset"))
                .findFirst().orElseThrow();

        // Pedido de exemplo 1 - ja entregue (ENTREGUE), para RF0025 e para
        // poder exercitar o fluxo de troca (RF0041 a RF0045) logo de cara.
        Pedido pedidoEntregue = Pedido.builder()
                .cliente(joao)
                .status(StatusPedido.ENTREGUE)
                .enderecoEntrega(endereco)
                .frete(new BigDecimal("24.90"))
                .valorTotal(new BigDecimal("1274.90"))
                .dataCriacao(LocalDateTime.now().minusDays(10))
                .build();
        pedidoEntregue.getItens().add(ItemPedido.builder()
                .pedido(pedidoEntregue)
                .produto(ssd)
                .quantidade(1)
                .precoUnitario(ssd.getPrecoVenda())
                .build());
        pedidoEntregue.getPagamentos().add(PagamentoPedido.builder()
                .pedido(pedidoEntregue)
                .cartao(cartao)
                .valor(new BigDecimal("1274.90"))
                .build());
        pedidoRepository.save(pedidoEntregue);

        // Pedido de exemplo 2 - a caminho (EM_TRANSPORTE).
        Pedido pedidoEmTransporte = Pedido.builder()
                .cliente(joao)
                .status(StatusPedido.EM_TRANSPORTE)
                .enderecoEntrega(endereco)
                .frete(new BigDecimal("24.90"))
                .valorTotal(new BigDecimal("374.80"))
                .dataCriacao(LocalDateTime.now().minusDays(2))
                .build();
        pedidoEmTransporte.getItens().add(ItemPedido.builder()
                .pedido(pedidoEmTransporte)
                .produto(headset)
                .quantidade(1)
                .precoUnitario(headset.getPrecoVenda())
                .build());
        pedidoEmTransporte.getPagamentos().add(PagamentoPedido.builder()
                .pedido(pedidoEmTransporte)
                .cartao(cartao)
                .valor(new BigDecimal("374.80"))
                .build());
        pedidoRepository.save(pedidoEmTransporte);
    }
}
