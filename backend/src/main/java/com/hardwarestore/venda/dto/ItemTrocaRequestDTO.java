package com.hardwarestore.venda.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemTrocaRequestDTO {

    @NotNull(message = "Informe o item do pedido.")
    private Long itemPedidoId;

    @NotNull(message = "Informe a quantidade a trocar.")
    @Min(value = 1, message = "A quantidade deve ser maior que zero.")
    private Integer quantidade;
}
