package com.hardwarestore.venda.dto;

import com.hardwarestore.venda.model.ItemCarrinho;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItemCarrinhoResponseDTO {

    private Long id;
    private Long produtoId;
    private String produtoNome;
    private BigDecimal precoUnitario;
    private Integer quantidade;
    private BigDecimal subtotal;

    public static ItemCarrinhoResponseDTO fromEntity(ItemCarrinho i) {
        BigDecimal subtotal = i.getProduto().getPrecoVenda().multiply(BigDecimal.valueOf(i.getQuantidade()));
        return new ItemCarrinhoResponseDTO(
                i.getId(),
                i.getProduto().getId(),
                i.getProduto().getNome(),
                i.getProduto().getPrecoVenda(),
                i.getQuantidade(),
                subtotal
        );
    }
}
