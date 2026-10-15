package com.hardwarestore.venda.model;

/**
 * Os 8 estados de uma venda definidos no DRS (grupo Gerenciar Vendas
 * Eletronicas) e no CDU02 - Gerenciar Venda Eletronica.
 *
 * Fluxo: EM_PROCESSAMENTO (nasce na finalizacao da compra, RF0038) ->
 * validacao de pagamento (RN0037/RN0038) -> APROVADA (com baixa em
 * estoque, RF0053) ou REPROVADA (estoque preservado, RN0028) -> a partir
 * de APROVADA, despacho (RF0039) -> EM_TRANSPORTE -> confirmacao de
 * entrega (RF0040) -> ENTREGUE. A partir de ENTREGUE, o cliente pode
 * solicitar troca (RF0041): EM_TROCA -> autorizacao do admin (RF0042) ->
 * TROCA_AUTORIZADA -> recebimento do item devolvido (RF0044) -> TROCADO
 * (com reentrada em estoque condicional, RF0054, e cupom de troca,
 * RF0045).
 */
public enum StatusPedido {
    EM_PROCESSAMENTO,
    APROVADA,
    REPROVADA,
    EM_TRANSPORTE,
    ENTREGUE,
    EM_TROCA,
    TROCA_AUTORIZADA,
    TROCADO
}

