package com.hardwarestore.venda.dto;

import com.hardwarestore.venda.model.ItemSolicitacaoTroca;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItemSolicitacaoTrocaResponseDTO {

    private Long itemPedidoId;
    private String produtoNome;
    private Integer quantidade;
    private BigDecimal valor;

    public static ItemSolicitacaoTrocaResponseDTO fromEntity(ItemSolicitacaoTroca i) {
        BigDecimal valor = i.getItemPedido().getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade()));
        return new ItemSolicitacaoTrocaResponseDTO(
                i.getItemPedido().getId(),
                i.getItemPedido().getProduto().getNome(),
                i.getQuantidade(),
                valor
        );
    }
}
