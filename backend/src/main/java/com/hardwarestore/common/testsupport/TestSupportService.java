package com.hardwarestore.common.testsupport;

import com.hardwarestore.cliente.config.DataSeeder;
import com.hardwarestore.common.exception.RecursoNaoEncontradoException;
import com.hardwarestore.venda.config.CupomSeeder;
import com.hardwarestore.venda.config.ProdutoSeeder;
import com.hardwarestore.venda.model.Carrinho;
import com.hardwarestore.venda.model.Produto;
import com.hardwarestore.venda.repository.CarrinhoRepository;
import com.hardwarestore.venda.repository.ProdutoRepository;
import com.hardwarestore.venda.service.CarrinhoExpiracaoJob;
import com.hardwarestore.venda.service.PagamentoValidacaoJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SUPORTE A TESTES AUTOMATIZADOS - nunca faz parte do comportamento do
 * sistema. So existe quando app.test-support.enabled=true (padrao no perfil
 * H2/desenvolvimento; desligado no perfil "postgres").
 *
 * Permite que cada suite Cypress comece de um estado conhecido (a base
 * volta ao que os seeders criam, com os mesmos ids de sempre) e que os
 * testes forcem situacoes dificeis de produzir pela interface - como o
 * estoque mudar no meio da compra (RN0032) ou o prazo de bloqueio do
 * carrinho vencer (RN0044) - sem esperar 30 minutos de verdade.
 */
@Service
@ConditionalOnProperty(name = "app.test-support.enabled", havingValue = "true")
public class TestSupportService {

    private final JdbcTemplate jdbc;
    private final ProdutoSeeder produtoSeeder;
    private final DataSeeder dataSeeder;
    private final CupomSeeder cupomSeeder;
    private final ProdutoRepository produtoRepository;
    private final CarrinhoRepository carrinhoRepository;
    private final CarrinhoExpiracaoJob expiracaoJob;
    private final PagamentoValidacaoJob pagamentoJob;

    public TestSupportService(JdbcTemplate jdbc,
                              ProdutoSeeder produtoSeeder,
                              DataSeeder dataSeeder,
                              CupomSeeder cupomSeeder,
                              ProdutoRepository produtoRepository,
                              CarrinhoRepository carrinhoRepository,
                              CarrinhoExpiracaoJob expiracaoJob,
                              PagamentoValidacaoJob pagamentoJob) {
        this.jdbc = jdbc;
        this.produtoSeeder = produtoSeeder;
        this.dataSeeder = dataSeeder;
        this.cupomSeeder = cupomSeeder;
        this.produtoRepository = produtoRepository;
        this.carrinhoRepository = carrinhoRepository;
        this.expiracaoJob = expiracaoJob;
        this.pagamentoJob = pagamentoJob;
    }

    /** Apaga tudo, zera os contadores de id e roda os seeders na mesma ordem da subida (1, 2, 3). */
    public void resetar() {
        String banco = jdbc.execute((ConnectionCallback<String>) c -> c.getMetaData().getDatabaseProductName());
        if ("H2".equalsIgnoreCase(banco)) {
            List<String> tabelas = jdbc.queryForList(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                            + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'", String.class);
            jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
            try {
                for (String tabela : tabelas) {
                    jdbc.execute("TRUNCATE TABLE \"" + tabela + "\" RESTART IDENTITY");
                }
            } finally {
                jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
            }
        } else {
            List<String> tabelas = jdbc.queryForList(
                    "SELECT table_name FROM information_schema.tables "
                            + "WHERE table_schema = current_schema() AND table_type = 'BASE TABLE'", String.class);
            if (!tabelas.isEmpty()) {
                String lista = String.join(", ", tabelas.stream().map(t -> "\"" + t + "\"").toList());
                jdbc.execute("TRUNCATE TABLE " + lista + " RESTART IDENTITY CASCADE");
            }
        }
        produtoSeeder.run();
        dataSeeder.run();
        cupomSeeder.run();
    }

    /** Define o estoque de um produto (simula outro cliente comprando, reposicao, etc.). */
    @Transactional
    public void definirEstoque(Long produtoId, int quantidade) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto nao encontrado. ID: " + produtoId));
        produto.setQuantidadeEstoque(quantidade);
        produtoRepository.save(produto);
    }

    /**
     * Ajusta o prazo de bloqueio de todos os itens do carrinho do cliente
     * para "agora + segundos" (negativo = ja vencido), sem esperar o prazo real.
     */
    @Transactional
    public void definirBloqueio(Long clienteId, long segundos) {
        Carrinho carrinho = carrinhoRepository.findByClienteId(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Este cliente ainda nao tem um carrinho."));
        LocalDateTime ate = LocalDateTime.now().plusSeconds(segundos);
        carrinho.getItens().forEach(i -> i.setBloqueadoAte(ate));
        carrinhoRepository.save(carrinho);
    }

    /** Liga/desliga (negativo) a validacao automatica do pagamento e ajusta o atraso, em segundos. */
    public void definirAtrasoValidacaoPagamento(int segundos) {
        pagamentoJob.definirAtrasoSegundos(segundos);
    }

    /** Valida agora o pagamento dos pedidos EM_PROCESSAMENTO, como o job faria depois do atraso. */
    public int validarPagamentosAgora(Long pedidoId) {
        return pagamentoJob.validarAgora(pedidoId);
    }

    /** Roda o job de expiracao agora, em vez de esperar o proximo ciclo agendado. */
    public int varrerExpirados() {
        return expiracaoJob.varrerAgora();
    }
}
