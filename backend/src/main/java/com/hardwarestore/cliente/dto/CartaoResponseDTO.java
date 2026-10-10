package com.hardwarestore.cliente.dto;

import com.hardwarestore.cliente.model.Bandeira;
import com.hardwarestore.cliente.model.CartaoCredito;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CartaoResponseDTO {

    private Long id;
    private Bandeira bandeira;
    private String ultimosQuatroDigitos;
    private String titular;
    private boolean preferencial;

    public static CartaoResponseDTO fromEntity(CartaoCredito c) {
        return new CartaoResponseDTO(
                c.getId(), c.getBandeira(), c.getUltimosQuatroDigitos(), c.getTitular(), c.isPreferencial()
        );
    }
}
