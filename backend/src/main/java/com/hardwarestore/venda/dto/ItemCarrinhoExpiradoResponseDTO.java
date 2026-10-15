package com.hardwarestore.venda.dto;

import com.hardwarestore.venda.model.ItemCarrinhoExpirado;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** RNF0042 - item retirado do carrinho por expiracao do bloqueio. */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItemCarrinhoExpiradoResponseDTO {

    private Long id;
    private Long produtoId;
    private String produtoNome;
    private Integer quantidade;
    private LocalDateTime expiradoEm;

    public static ItemCarrinhoExpiradoResponseDTO fromEntity(ItemCarrinhoExpirado e) {
        return new ItemCarrinhoExpiradoResponseDTO(
                e.getId(),
                e.getProduto().getId(),
                e.getProduto().getNome(),
                e.getQuantidade(),
                e.getExpiradoEm()
        );
    }
}
