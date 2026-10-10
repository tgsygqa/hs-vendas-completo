package com.hardwarestore.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Centraliza a traducao de excecoes de validacao e de negocio para
 * respostas HTTP em JSON limpo, com o status e a mensagem corretos - nenhum
 * Controller precisa de try/catch. Movido de com.hardwarestore.cliente para
 * cá porque agora atende tanto o modulo de Clientes quanto o de Vendas.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // RNF - mensagens de erro adequadas para campos obrigatorios/formato
    // invalido (Bean Validation nos DTOs) -> 400 Bad Request
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new HashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.put(erro.getField(), erro.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(corpoErro(HttpStatus.BAD_REQUEST, "Dados invalidos.", campos));
    }

    // Regras de negocio violadas (CPF/e-mail duplicado, senhas divergentes,
    // bandeira nao registrada, endereco de cobranca/entrega obrigatorio,
    // exclusao bloqueada por haver transacoes, estoque insuficiente,
    // pagamento reprovado) -> 409 Conflict
    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<Map<String, Object>> tratarRegraDeNegocio(RegraDeNegocioException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(corpoErro(HttpStatus.CONFLICT, ex.getMessage(), null));
    }

    // Cliente/endereco/cartao/produto/carrinho/pedido nao encontrado -> 404 Not Found
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(corpoErro(HttpStatus.NOT_FOUND, ex.getMessage(), null));
    }

    // JSON malformado ou valor de enum invalido (ex.: genero, tipo de
    // telefone ou status de pedido fora dos valores aceitos) -> 400 Bad
    // Request, em vez do stack trace generico que o Spring devolveria por
    // padrao
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> tratarJsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(corpoErro(HttpStatus.BAD_REQUEST, "Corpo da requisicao invalido ou com valor nao aceito.", null));
    }

    private Map<String, Object> corpoErro(HttpStatus status, String mensagem, Map<String, String> campos) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("timestamp", LocalDateTime.now());
        corpo.put("status", status.value());
        corpo.put("mensagem", mensagem);
        if (campos != null && !campos.isEmpty()) {
            corpo.put("campos", campos);
        }
        return corpo;
    }
}
