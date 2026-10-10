package com.hardwarestore.common.exception;

/**
 * Lancada quando uma regra de negocio e violada: CPF/e-mail duplicado
 * (RF0021/RF0022), senha e confirmacao divergentes (RNF0032), bandeira nao
 * registrada (RN0025), tentativa de deixar o cliente sem endereco de
 * cobranca/entrega (RN0021/RN0022), exclusao bloqueada por haver
 * transacoes associadas, estoque insuficiente (RN0031/RN0032), forma de
 * pagamento reprovada (RN0037/RN0038), etc.
 *
 * Movida de com.hardwarestore.cliente.exception para cá porque, a partir do
 * modulo de Vendas, deixou de ser algo exclusivo do Cliente: as duas areas
 * da aplicacao agora compartilham o mesmo tipo de excecao e o mesmo
 * tratamento central (ver GlobalExceptionHandler). Mapeada para HTTP 409
 * (Conflict).
 */
public class RegraDeNegocioException extends RuntimeException {
    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
