package com.hardwarestore.venda.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * RF0044 - "o administrador devera informar se os itens trocados deverao
 * retornar ao estoque".
 */
@Getter
@Setter
public class ConfirmarRecebimentoTrocaRequestDTO {

    @NotNull(message = "Informe se os itens devem retornar ao estoque.")
    private Boolean itensRetornaramEstoque;
}
