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

    public static CupomResponseDTO fromEntity(Cupom c) {
        return new CupomResponseDTO(c.getId(), c.getCodigo(), c.getTipo().name(), c.getValor());
    }
}
