package com.hardwarestore.venda.model;

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
 * RNF0042 - registro de um item que foi retirado do carrinho porque o prazo
 * de bloqueio (RN0044) acabou. O DRS exige que esses produtos continuem
 * aparecendo na listagem do carrinho (com a opcao de comprar desabilitada)
 * ate o cliente adiciona-los de novo - por isso nao basta apagar o
 * ItemCarrinho: este registro guarda o que saiu e quando.
 */
@Entity
@Table(name = "itens_carrinho_expirados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemCarrinhoExpirado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrinho_id", nullable = false)
    private Carrinho carrinho;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    // Quantidade que o cliente tinha no carrinho quando o bloqueio expirou.
    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false)
    private LocalDateTime expiradoEm;
}
