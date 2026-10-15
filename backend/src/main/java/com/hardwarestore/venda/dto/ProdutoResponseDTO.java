package com.hardwarestore.venda.dto;

import com.hardwarestore.venda.model.Produto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProdutoResponseDTO {

    private Long id;
    private String nome;
    private String categoria;
    private String fornecedor;
    private BigDecimal precoVenda;

    // RN0031/RN0044 - estoque menos o que esta bloqueado (reservado) em
    // carrinhos de OUTROS clientes com prazo ainda valido.
    private Integer quantidadeDisponivel;

    // Estoque fisico (RF0053/RF0054) - usado pelo painel do administrador.
    private Integer quantidadeEstoque;

    public static ProdutoResponseDTO fromEntity(Produto p, int quantidadeDisponivel) {
        return new ProdutoResponseDTO(
                p.getId(),
                p.getNome(),
                p.getCategoria(),
                p.getFornecedor(),
                p.getPrecoVenda(),
                quantidadeDisponivel,
                p.getQuantidadeEstoque()
        );
    }
}
