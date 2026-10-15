package com.hardwarestore.venda.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.hardwarestore.cliente.model.Cliente;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * RF0037 - cupom de troca ou promocional usado como forma de pagamento.
 *
 * Um cupom nasce de tres formas: carregado pelo CupomSeeder (cupons
 * promocionais e de troca da demonstracao), como cupom de TROCO quando os
 * cupons usados superam o valor da compra (RN0036, PedidoService.finalizarCompra)
 * ou como cupom de TROCA gerado ao confirmar o recebimento dos itens
 * devolvidos (RF0045, PedidoService.confirmarRecebimentoTroca).
 *
 * cliente nulo = cupom promocional de uso geral, nao vinculado a ninguem
 * especifico. Cupom de troca sempre pertence a um cliente.
 */
@Entity
@Table(name = "cupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cupom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCupom tipo;

    @Column(nullable = false)
    private BigDecimal valor;

    // RN0033/RN0036 - uma vez usado (em qualquer pedido), nao pode ser
    // usado de novo.
    @Column(nullable = false)
    @Builder.Default
    private boolean utilizado = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    // Preenchido quando o cupom e consumido por um pedido (ver
    // PedidoService) - inclui os cupons de troco gerados pela RN0036.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_utilizacao_id")
    @JsonBackReference
    private Pedido pedidoUtilizacao;
}

