package com.hardwarestore.venda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * RF0035 - endereco de entrega e RF0036/RF0037 - forma de pagamento,
 * escolhidos entre os ja cadastrados no perfil do cliente (RF0026/RF0027)
 * ou recem-cadastrados durante a compra (o front cadastra primeiro via
 * POST /api/clientes/{id}/enderecos ou /cartoes, que ja incorporam ao
 * perfil, e so entao manda o id aqui).
 *
 * cartoes pode vir vazio se os cupons sozinhos cobrirem o valor total
 * (RN0036); cupomIds e opcional. Pelo menos um dos dois precisa cobrir o
 * valor da compra - ver PedidoService.
 */
@Getter
@Setter
public class FinalizarCompraRequestDTO {

    @NotNull(message = "Selecione o endereco de entrega.")
    private Long enderecoId;

    // Pode vir vazia/nula se os cupons sozinhos cobrirem o valor total
    // (RN0036) - a validacao de que a soma de tudo cobre a compra fica no
    // PedidoService, nao aqui.
    @Valid
    private List<PagamentoCartaoRequestDTO> cartoes;

    private List<Long> cupomIds;
}
