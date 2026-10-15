package com.hardwarestore.venda.service;

import com.hardwarestore.venda.model.PagamentoPedido;
import com.hardwarestore.venda.model.Pedido;
import org.springframework.stereotype.Service;

/**
 * RN0037 - "validar a validade e a veracidade dos cupons utilizados, bem
 * como o aceite da compra pela operadora do cartao de credito". Nao existe
 * (nem deveria existir, num projeto academico) integracao real com uma
 * operadora de cartao - esta classe SIMULA o retorno dela.
 *
 * Regra da simulacao (deterministica, para poder demonstrar tanto o fluxo
 * de sucesso quanto o E3 do CDU - pagamento REPROVADO): a "operadora"
 * RECUSA qualquer compra em que ao menos um dos cartoes termine em
 * {@value #FINAL_CARTAO_RECUSADO}. Qualquer outro final e aprovado. Para
 * demonstrar a reprovacao, basta cadastrar um cartao com numero terminado
 * em 0000 (ex.: 4111111111110000) e paga-lo na compra.
 *
 * Compras pagas so com cupons nao passam por cartao e sao sempre aprovadas.
 */
@Service
public class PagamentoGatewayService {

    public static final String FINAL_CARTAO_RECUSADO = "0000";

    /** Resultado da consulta a "operadora". motivo so e preenchido na recusa. */
    public record Autorizacao(boolean aprovada, String motivo) {
    }

    public Autorizacao autorizar(Pedido pedido) {
        for (PagamentoPedido pagamento : pedido.getPagamentos()) {
            String final4 = pagamento.getCartao().getUltimosQuatroDigitos();
            if (FINAL_CARTAO_RECUSADO.equals(final4)) {
                return new Autorizacao(false,
                        "Cartão final " + final4 + " recusado pela operadora.");
            }
        }
        return new Autorizacao(true, null);
    }
}
