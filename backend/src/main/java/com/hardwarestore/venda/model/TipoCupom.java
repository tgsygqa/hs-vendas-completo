package com.hardwarestore.venda.model;

/**
 * RF0037 - os dois tipos de cupom que o DRS prevê como forma de pagamento.
 * PROMOCIONAL - no máximo um por compra (RN0033). TROCA - gerado a partir
 * de uma devolução (RF0045, fora de escopo nesta entrega) ou como troco
 * quando os cupons usados superam o valor da compra (RN0036, que esta
 * dentro do escopo, apesar de o processo de troca em si nao estar).
 */
public enum TipoCupom {
    PROMOCIONAL,
    TROCA
}
