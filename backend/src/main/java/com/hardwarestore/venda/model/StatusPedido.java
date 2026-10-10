package com.hardwarestore.venda.model;

/**
 * Os 8 estados de uma venda definidos no DRS (grupo Gerenciar Vendas
 * Eletronicas) e no CDU02 - Gerenciar Venda Eletronica. Por instrucao do
 * professor, o fluxo de sucesso (entrega de 06/10) so alcanca
 * EM_PROCESSAMENTO: a validacao de pagamento (RN0037/RN0038) e tudo que
 * vem depois dela - APROVADA, REPROVADA, EM_TRANSPORTE, ENTREGUE e todo o
 * processo de troca - estao fora do escopo desta fase. Os 8 valores
 * existem aqui desde ja para o enum nascer completo e fiel ao DRS, mas sao
 * retomados na entrega de 20/10, com o caso de uso completo.
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
