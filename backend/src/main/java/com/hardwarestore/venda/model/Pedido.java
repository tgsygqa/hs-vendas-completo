package com.hardwarestore.venda.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.hardwarestore.cliente.model.Cliente;
import com.hardwarestore.cliente.model.Endereco;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade central do modulo de Vendas - nasce quando o carrinho e
 * finalizado (RF0038) e percorre todos os estados de StatusPedido: nasce
 * em EM_PROCESSAMENTO, passa pela validacao de pagamento (RN0037/RN0038,
 * -> APROVADA com baixa em estoque RF0053, ou REPROVADA), despacho e
 * entrega (RF0039/RF0040, -> EM_TRANSPORTE -> ENTREGUE) e, a partir de
 * ENTREGUE, pelo processo de troca completo (RF0041-RF0045, detalhado nas
 * solicitacoes em solicitacoesTroca: EM_TROCA -> TROCA_AUTORIZADA ->
 * TROCADO).
 *
 * Esta classe substitui a versao minima que antes vivia em
 * com.hardwarestore.cliente.model.Pedido, criada so para o RF0025 (consulta
 * de transacoes) funcionar antes do modulo de Vendas existir de verdade.
 * RF0025 continua atendido por ela (ver PedidoRepository.findByClienteId),
 * so que agora pelo objeto completo.
 *
 * Simplificacao assumida (documentar se perguntado): o enderecoEntrega
 * aqui e uma referencia viva ao Endereco do cliente, nao um instantaneo.
 * Isso significa que, se o cliente editar esse endereco depois, pedidos
 * antigos passam a exibir o endereco editado. O jeito correto para
 * producao seria copiar os campos do endereco para o pedido no momento da
 * compra; ficou de fora por simplicidade e e a primeira melhoria a fazer.
 */
@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPedido status;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<ItemPedido> itens = new ArrayList<>();

    // RF0035 - endereco de entrega selecionado (ver observacao da classe
    // sobre esta ser uma referencia viva, nao um instantaneo).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endereco_entrega_id", nullable = false)
    private Endereco enderecoEntrega;

    // RF0034 - frete calculado para esta compra.
    @Column(nullable = false)
    private BigDecimal frete;

    // Soma dos itens (precoUnitario x quantidade) mais o frete, no momento
    // da finalizacao.
    @Column(nullable = false)
    private BigDecimal valorTotal;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    // RN0038 - quando o pagamento e reprovado, guarda o motivo devolvido pela
    // "operadora" (ver PagamentoGatewayService) para exibir ao cliente.
    @Column(length = 500)
    private String motivoReprovacao;

    // RF0036/RN0034 - um pedido pode ser pago com mais de um cartao, cada
    // um com seu proprio valor.
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<PagamentoPedido> pagamentos = new ArrayList<>();

    // RF0037/RN0033/RN0036 - cupons (promocionais ou de troca) usados
    // nesta compra, incluindo um eventual cupom de troco gerado por
    // RN0036 (esse ultimo fica com pedidoUtilizacao apontando para ESTE
    // pedido, mas sem ter sido "gasto" para pagar - e o contrario, foi
    // gerado por causa dele).
    @OneToMany(mappedBy = "pedidoUtilizacao")
    @JsonManagedReference
    @Builder.Default
    private List<Cupom> cuponsUtilizados = new ArrayList<>();

    // RF0041-RF0045 - solicitacoes de troca abertas para este pedido (um
    // pedido ENTREGUE pode ter mais de uma, uma por vez em aberto na
    // pratica, mas a lista fica aberta para o historico completo).
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<SolicitacaoTroca> solicitacoesTroca = new ArrayList<>();
}

