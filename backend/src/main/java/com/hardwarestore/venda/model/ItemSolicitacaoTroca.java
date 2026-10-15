package com.hardwarestore.venda.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
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

/**
 * RF0041 - um dos itens (e a quantidade) de um ItemPedido original que o
 * cliente quer trocar. quantidade pode ser menor que a quantidade
 * originalmente comprada naquele item - RN0041 permite trocar "um ou mais
 * itens" ou a compra toda, nao exige que seja tudo.
 */
@Entity
@Table(name = "itens_solicitacao_troca")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemSolicitacaoTroca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    @JsonBackReference
    private SolicitacaoTroca solicitacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_pedido_id", nullable = false)
    private ItemPedido itemPedido;

    @Column(nullable = false)
    private Integer quantidade;
}
