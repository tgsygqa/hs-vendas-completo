package com.hardwarestore.venda.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita as tarefas agendadas (@Scheduled) - hoje, so o job que libera os
 * itens de carrinho cujo bloqueio expirou (RN0044/RN0045, ver
 * CarrinhoExpiracaoJob).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
