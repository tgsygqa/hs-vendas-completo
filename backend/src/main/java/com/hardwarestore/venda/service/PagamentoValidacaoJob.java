package com.hardwarestore.venda.service;

import com.hardwarestore.venda.model.Pedido;
import com.hardwarestore.venda.model.StatusPedido;
import com.hardwarestore.venda.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RN0037/RN0038 - comportamento AUTOMATICO do sistema: depois que a compra e
 * finalizada (EM PROCESSAMENTO, RF0038), o sistema valida sozinho a forma de
 * pagamento (cupons + aceite da operadora, simulado em
 * PagamentoGatewayService) e muda o status para APROVADA ou REPROVADA. Nenhum
 * usuario aciona isso.
 *
 * O job varre os pedidos EM PROCESSAMENTO criados ha pelo menos
 * app.pagamento.validacao-segundos (padrao 10 s: o "tempo de resposta da
 * operadora", que deixa o status EM PROCESSAMENTO visivel na tela). Valor
 * negativo desliga a validacao automatica (usado so nos testes).
 */
@Component
public class PagamentoValidacaoJob {

    private static final Logger log = LoggerFactory.getLogger(PagamentoValidacaoJob.class);

    private final PedidoRepository pedidoRepository;
    private final PedidoService pedidoService;
    private final AtomicInteger atrasoSegundos;

    public PagamentoValidacaoJob(PedidoRepository pedidoRepository,
                                 PedidoService pedidoService,
                                 @Value("${app.pagamento.validacao-segundos:10}") int atrasoSegundos) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoService = pedidoService;
        this.atrasoSegundos = new AtomicInteger(atrasoSegundos);
    }

    @Scheduled(fixedDelayString = "#{${app.pagamento.varredura-segundos:2} * 1000}",
               initialDelayString = "#{${app.pagamento.varredura-segundos:2} * 1000}")
    public void validarPendentes() {
        int atraso = atrasoSegundos.get();
        if (atraso < 0) return;   // validacao automatica desligada
        validar(pedidoRepository.findByStatusAndDataCriacaoLessThanEqual(
                StatusPedido.EM_PROCESSAMENTO, LocalDateTime.now().minusSeconds(atraso)));
    }

    /** Suporte a testes: valida agora os pedidos EM PROCESSAMENTO (todos, ou so um), sem esperar o atraso. */
    public int validarAgora(Long pedidoId) {
        List<Pedido> pendentes = pedidoId == null
                ? pedidoRepository.findByStatusOrderByIdDesc(StatusPedido.EM_PROCESSAMENTO)
                : pedidoRepository.findById(pedidoId).filter(p -> p.getStatus() == StatusPedido.EM_PROCESSAMENTO).stream().toList();
        return validar(pendentes);
    }

    /** Suporte a testes: muda o atraso da validacao automatica (negativo = desligada). */
    public void definirAtrasoSegundos(int segundos) {
        atrasoSegundos.set(segundos);
    }

    private int validar(List<Pedido> pedidos) {
        int validados = 0;
        for (Pedido p : pedidos) {
            try {
                pedidoService.validarPagamento(p.getId());
                validados++;
            } catch (RuntimeException e) {
                // um pedido com problema nao pode travar os demais; ele sera tentado de novo no proximo ciclo
                log.warn("Falha ao validar o pagamento do pedido {}: {}", p.getId(), e.getMessage());
            }
        }
        if (validados > 0) log.info("Pagamento validado automaticamente para {} pedido(s).", validados);
        return validados;
    }
}
