package com.hardwarestore.venda.model;

/**
 * RF0037 - os dois tipos de cupom que o DRS prevê como forma de pagamento.
 * PROMOCIONAL - no máximo um por compra (RN0033). TROCA - gerado a partir
 * de uma devolução (RF0045, ver PedidoService.confirmarRecebimentoTroca) ou
 * como troco quando os cupons usados superam o valor da compra (RN0036).
 */
public enum TipoCupom {
    PROMOCIONAL,
    TROCA
}

