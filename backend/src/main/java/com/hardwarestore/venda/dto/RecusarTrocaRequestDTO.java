package com.hardwarestore.venda.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecusarTrocaRequestDTO {

    @NotBlank(message = "Informe o motivo da recusa.")
    private String motivoRecusa;
}
