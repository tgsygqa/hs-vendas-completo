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

import java.time.LocalDateTime;

/**
 * RF0031/RF0032 - um produto e a quantidade escolhida dentro do carrinho de
 * um cliente. RN0031 - a quantidade so e aceita se houver estoque
 * disponivel no momento da adicao.
 *
 * RN0044/RN0045 - bloqueadoAte registra ate quando a quantidade reservada
 * aqui fica indisponivel para outros clientes. O CarrinhoService o
 * preenche e renova a cada item incluido (o prazo e relativo ao ultimo item) e
 * o CarrinhoExpiracaoService/Job libera os itens vencidos (ver tambem
 * ItemCarrinhoExpirado, para RNF0042).
 */
@Entity
@Table(name = "itens_carrinho")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemCarrinho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrinho_id", nullable = false)
    @JsonBackReference
    private Carrinho carrinho;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private Integer quantidade;

    // RN0044 - prazo ate quando a quantidade deste item fica bloqueada em
    // estoque para outros clientes (ver javadoc da classe).
    private LocalDateTime bloqueadoAte;
}
