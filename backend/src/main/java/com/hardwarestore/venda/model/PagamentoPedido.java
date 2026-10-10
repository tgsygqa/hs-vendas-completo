package com.hardwarestore.venda.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.hardwarestore.cliente.model.CartaoCredito;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * RF0036 - um cartao de credito (ja cadastrado no cliente, RF0027) usado
 * para pagar parte ou o total de um Pedido. RN0034 permite varios cartoes
 * por compra, desde que cada um pague ao menos R$ 10,00 - o fluxo de
 * sucesso usa um unico cartao, mas a entidade ja fica pronta para mais.
 */
@Entity
@Table(name = "pagamentos_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagamentoPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    @JsonBackReference
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartao_id", nullable = false)
    private CartaoCredito cartao;

    @Column(nullable = false)
    private BigDecimal valor;
}
