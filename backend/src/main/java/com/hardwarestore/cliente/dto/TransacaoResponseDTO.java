package com.hardwarestore.cliente.dto;

import com.hardwarestore.venda.model.Pedido;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TransacaoResponseDTO {

    private Long id;
    private String status;
    private BigDecimal total;

    public static TransacaoResponseDTO fromEntity(Pedido p) {
        return new TransacaoResponseDTO(p.getId(), p.getStatus().name(), p.getValorTotal());
    }
}
