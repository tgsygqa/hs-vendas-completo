package com.hardwarestore.venda.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlterarQuantidadeRequestDTO {

    @NotNull(message = "Informe a quantidade.")
    @Min(value = 1, message = "A quantidade deve ser maior que zero.")
    private Integer quantidade;
}
