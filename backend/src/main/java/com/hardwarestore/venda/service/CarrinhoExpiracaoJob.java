package com.hardwarestore.venda.service;

import com.hardwarestore.venda.repository.CarrinhoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RN0044 - "se o tempo limite expirar, os itens serao removidos e
 * desbloqueados para outros clientes". Varre periodicamente os carrinhos
 * com itens vencidos (intervalo em app.carrinho.varredura-segundos). Mesmo
 * sem este job, o carrinho tambem e conferido a cada acesso (ver
 * CarrinhoService), entao o job so garante que o estoque seja liberado para
 * os outros clientes mesmo que o dono do carrinho nunca volte.
 */
@Component
public class CarrinhoExpiracaoJob {

    private static final Logger log = LoggerFactory.getLogger(CarrinhoExpiracaoJob.class);

    private final CarrinhoRepository carrinhoRepository;
    private final CarrinhoExpiracaoService expiracaoService;

    public CarrinhoExpiracaoJob(CarrinhoRepository carrinhoRepository,
                                CarrinhoExpiracaoService expiracaoService) {
        this.carrinhoRepository = carrinhoRepository;
        this.expiracaoService = expiracaoService;
    }

    @Scheduled(fixedDelayString = "#{${app.carrinho.varredura-segundos:30} * 1000}",
               initialDelayString = "#{${app.carrinho.varredura-segundos:30} * 1000}")
    public void varrer() {
        varrerAgora();
    }

    /** Tambem usado pelo endpoint de suporte a testes, para nao depender do relogio. */
    public int varrerAgora() {
        List<Long> ids = carrinhoRepository.findIdsComItemVencido(LocalDateTime.now());
        int total = 0;
        for (Long id : ids) {
            List<String> expirados = expiracaoService.expirarVencidosDoCarrinho(id);
            total += expirados.size();
            if (!expirados.isEmpty()) {
                log.info("Bloqueio expirado no carrinho {}: itens liberados = {}", id, expirados);
            }
        }
        return total;
    }
}
