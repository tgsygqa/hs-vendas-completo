package com.hardwarestore.common.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * RNF0012 - "toda insercao ou alteracao relacionada a compra (ou ao
 * cliente) deve registrar data, hora, usuario responsavel e os dados
 * alterados". Em vez de espalhar chamadas de log em cada metodo de
 * service, um unico aspecto intercepta TODO metodo @Transactional dos
 * modulos de Cliente e Vendas - que, pelo padrao ja usado no projeto
 * (ClienteService, CarrinhoService, PedidoService), sao exatamente os
 * metodos que escrevem no banco. Consultas (sem @Transactional) nao sao
 * interceptadas, so escritas.
 *
 * Limitacao assumida: o projeto nao tem autenticacao/login real (fora de
 * escopo do DRS para esta entrega), entao nao existe um "usuario logado"
 * de verdade para registrar. O log usa o primeiro argumento do metodo
 * (quase sempre o clienteId, dado o padrao das assinaturas no projeto)
 * como identificador de quem fez a operacao.
 */
@Aspect
@Component
public class LogDeEscritaAspect {

    private static final Logger log = LoggerFactory.getLogger("com.hardwarestore.OPERACOES_DE_ESCRITA");

    @Around("@annotation(org.springframework.transaction.annotation.Transactional) "
            + "&& (within(com.hardwarestore.cliente.service..*) || within(com.hardwarestore.venda.service..*))")
    public Object logar(ProceedingJoinPoint joinPoint) throws Throwable {
        String metodo = joinPoint.getSignature().toShortString();
        String argumentos = Arrays.toString(joinPoint.getArgs());
        LocalDateTime momento = LocalDateTime.now();

        try {
            Object resultado = joinPoint.proceed();
            log.info("data/hora={} | operacao={} | usuario(1o arg)={} | resultado=sucesso",
                    momento, metodo, argumentos);
            return resultado;
        } catch (Throwable erro) {
            log.warn("data/hora={} | operacao={} | usuario(1o arg)={} | resultado=falha | motivo={}",
                    momento, metodo, argumentos, erro.getMessage());
            throw erro;
        }
    }
}
