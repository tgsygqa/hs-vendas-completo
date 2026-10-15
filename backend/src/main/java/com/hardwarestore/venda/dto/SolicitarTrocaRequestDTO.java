package com.hardwarestore.venda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * RF0041 - um ou mais itens de uma compra ENTREGUE (RN0043), com o motivo
 * que o cliente informa.
 */
@Getter
@Setter
public class SolicitarTrocaRequestDTO {

    @NotEmpty(message = "Selecione ao menos um item para trocar.")
    @Valid
    private List<ItemTrocaRequestDTO> itens;

    @NotBlank(message = "Informe o motivo da troca.")
    private String motivoCliente;
}
