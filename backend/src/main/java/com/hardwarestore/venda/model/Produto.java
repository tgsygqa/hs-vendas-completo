package com.hardwarestore.venda.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Representacao minima de um produto, apenas o suficiente para dar suporte
 * ao carrinho e a venda (RF0031 em diante). O cadastro completo de produto
 * - no DRS, "Cadastro de Livros", RF0011 a RF0016, com autor, ISBN,
 * editora, grupo de precificacao com margem de lucro, motivo de
 * inativacao, etc. - e um caso de uso separado, de outra dupla do grupo, e
 * fica fora do escopo deste modulo. Aqui os produtos sao semeados
 * diretamente (ver ProdutoSeeder) com os campos que a tela de carrinho
 * realmente usa: nome, categoria, fornecedor, preco e estoque.
 *
 * quantidadeEstoque e o estoque FISICO: so muda na baixa da aprovacao do
 * pagamento (RF0053) e na reentrada de uma troca (RF0054). Nao existe um
 * contador de reserva separado: o que esta bloqueado nos carrinhos
 * (RN0044) e calculado na hora a partir dos ItemCarrinho com prazo ainda
 * valido (ver ProdutoService.quantidadeDisponivelPara) e descontado do
 * estoque disponivel para os OUTROS clientes.
 */
@Entity
@Table(name = "produtos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String categoria;

    @Column(nullable = false)
    private String fornecedor;

    // RF0052/RF0053 (Controle de Estoque) - preco pelo qual o produto e
    // vendido; o calculo do preco a partir de margem de lucro (RN0012 a
    // RN0017) pertence ao Cadastro de Livros/Produtos, fora de escopo aqui.
    @Column(nullable = false)
    private BigDecimal precoVenda;

    @Column(nullable = false)
    private Integer quantidadeEstoque;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;
}

