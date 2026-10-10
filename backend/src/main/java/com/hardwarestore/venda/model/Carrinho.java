package com.hardwarestore.venda.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.hardwarestore.cliente.model.Cliente;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * RF0031 - carrinho de compra: repositorio temporario de produtos
 * selecionados pelo cliente antes da efetivacao de uma compra. Cada
 * cliente tem no maximo um carrinho aberto por vez (por isso o
 * relacionamento 1-para-1, com unique na coluna de chave estrangeira).
 *
 * Quando a compra e finalizada (P5, RF0038), os itens daqui viram um
 * Pedido e este carrinho e esvaziado - ele nunca guarda historico, so o
 * estado atual.
 */
@Entity
@Table(name = "carrinhos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carrinho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private Cliente cliente;

    @OneToMany(mappedBy = "carrinho", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<ItemCarrinho> itens = new ArrayList<>();
}
