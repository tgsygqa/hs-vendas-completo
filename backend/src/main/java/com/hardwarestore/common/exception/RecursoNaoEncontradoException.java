package com.hardwarestore.common.exception;

/**
 * Lancada quando um cliente, endereco, cartao, produto, carrinho ou pedido
 * consultado nao existe. Movida de com.hardwarestore.cliente.exception para
 * cá pelo mesmo motivo da RegraDeNegocioException: passou a ser usada pelos
 * dois módulos. Mapeada pelo GlobalExceptionHandler para HTTP 404.
 */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
