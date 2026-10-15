package com.hardwarestore.venda.dto;

import com.hardwarestore.venda.model.Cupom;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CupomResponseDTO {

    private Long id;
    private String codigo;
    private String tipo;
    private BigDecimal valor;
    // true = ja foi gasto numa compra; false = disponivel (ex.: cupom de troco gerado pela RN0036).
    private boolean utilizado;

    public static CupomResponseDTO fromEntity(Cupom c) {
        return new CupomResponseDTO(c.getId(), c.getCodigo(), c.getTipo().name(), c.getValor(), c.isUtilizado());
    }
}

