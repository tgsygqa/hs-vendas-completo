package com.hardwarestore.venda.dto;

import com.hardwarestore.venda.model.Carrinho;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CarrinhoResponseDTO {

    private Long id;
    private List<ItemCarrinhoResponseDTO> itens;
    private BigDecimal total;

    public static CarrinhoResponseDTO fromEntity(Carrinho c) {
        List<ItemCarrinhoResponseDTO> itens = c.getItens().stream()
                .map(ItemCarrinhoResponseDTO::fromEntity)
                .toList();
        BigDecimal total = itens.stream()
                .map(ItemCarrinhoResponseDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CarrinhoResponseDTO(c.getId(), itens, total);
    }
}
