package com.hardwarestore.venda.dto;

import com.hardwarestore.venda.model.PagamentoPedido;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PagamentoResponseDTO {

    private Long cartaoId;
    private String cartaoBandeira;
    private String cartaoUltimosQuatroDigitos;
    private BigDecimal valor;

    public static PagamentoResponseDTO fromEntity(PagamentoPedido p) {
        return new PagamentoResponseDTO(
                p.getCartao().getId(),
                p.getCartao().getBandeira().name(),
                p.getCartao().getUltimosQuatroDigitos(),
                p.getValor()
        );
    }
}
