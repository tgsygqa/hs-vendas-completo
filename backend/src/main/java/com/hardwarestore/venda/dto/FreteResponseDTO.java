package com.hardwarestore.venda.dto;

import java.math.BigDecimal;

/**
 * RF0034 - resultado do calculo do frete para um endereco de entrega e a
 * quantidade de unidades do carrinho (ver FreteService para a regra).
 */
public record FreteResponseDTO(
        Long enderecoId,
        String uf,
        String regiao,
        int unidades,
        BigDecimal tarifaRegiao,
        BigDecimal adicionalUnidades,
        BigDecimal valor
) {
}
