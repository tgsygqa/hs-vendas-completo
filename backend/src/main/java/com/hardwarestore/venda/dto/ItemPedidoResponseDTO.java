package com.hardwarestore.venda.dto;

import com.hardwarestore.venda.model.ItemPedido;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItemPedidoResponseDTO {

    private Long id;
    private Long produtoId;
    private String produtoNome;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal subtotal;

    public static ItemPedidoResponseDTO fromEntity(ItemPedido i) {
        BigDecimal subtotal = i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade()));
        return new ItemPedidoResponseDTO(
                i.getId(),
                i.getProduto().getId(),
                i.getProduto().getNome(),
                i.getQuantidade(),
                i.getPrecoUnitario(),
                subtotal
        );
    }
}

