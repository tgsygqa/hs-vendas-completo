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

    // RN0031 - hoje e igual a quantidadeEstoque: nao ha reserva por item
    // em carrinho nesta entrega (RN0044/RN0045 fora de escopo). Mantido
    // como campo calculado, com este nome, para o front nao precisar
    // mudar quando a reserva for implementada na entrega de 20/10.
    private Integer quantidadeDisponivel;

    public static ProdutoResponseDTO fromEntity(Produto p) {
        return new ProdutoResponseDTO(
                p.getId(),
                p.getNome(),
                p.getCategoria(),
                p.getFornecedor(),
                p.getPrecoVenda(),
                p.getQuantidadeEstoque() - p.getQuantidadeReservada()
        );
    }
}
