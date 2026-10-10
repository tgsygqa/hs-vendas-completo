package com.hardwarestore.venda.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * RF0036/RN0034 - um cartao e o valor que ele vai pagar nesta compra. A
 * FinalizarCompraRequestDTO carrega uma lista destes, porque RN0034
 * permite pagar com mais de um cartao.
 */
@Getter
@Setter
public class PagamentoCartaoRequestDTO {

    @NotNull(message = "Informe o cartao.")
    private Long cartaoId;

    @NotNull(message = "Informe o valor a cobrar neste cartao.")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
    private BigDecimal valor;
}
