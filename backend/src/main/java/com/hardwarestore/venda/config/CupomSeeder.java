package com.hardwarestore.venda.config;

import com.hardwarestore.cliente.model.Cliente;
import com.hardwarestore.cliente.repository.ClienteRepository;
import com.hardwarestore.venda.model.Cupom;
import com.hardwarestore.venda.model.TipoCupom;
import com.hardwarestore.venda.repository.CupomRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * RF0037 - semeia cupons prontos para a demonstracao. O processo que GERA
 * um cupom de troca a partir de uma devolucao fisica (RF0045) esta fora do
 * escopo desta entrega; as instrucoes do professor autorizam
 * explicitamente carregar os cupons direto na base para a apresentacao.
 *
 * @Order(3) - depois do ProdutoSeeder (1) e do DataSeeder (2), porque os
 * cupons de troca abaixo pertencem ao Joao Silva, que so existe depois que
 * o DataSeeder roda.
 */
@Component
@Order(3)
public class CupomSeeder implements CommandLineRunner {

    private final CupomRepository cupomRepository;
    private final ClienteRepository clienteRepository;

    public CupomSeeder(CupomRepository cupomRepository, ClienteRepository clienteRepository) {
        this.cupomRepository = cupomRepository;
        this.clienteRepository = clienteRepository;
    }

    @Override
    public void run(String... args) {
        Cliente joao = clienteRepository.findAll().stream()
                .filter(c -> c.getCpf().equals("123.456.789-00"))
                .findFirst()
                .orElseThrow();

        // RN0033 - dois cupons promocionais, de uso geral (nao vinculados
        // a um cliente), para demonstrar que so um pode ser usado por
        // compra.
        cupomRepository.save(Cupom.builder()
                .codigo("PROMO-BEMVINDO")
                .tipo(TipoCupom.PROMOCIONAL)
                .valor(new BigDecimal("50.00"))
                .utilizado(false)
                .build());

        cupomRepository.save(Cupom.builder()
                .codigo("PROMO-VERAO")
                .tipo(TipoCupom.PROMOCIONAL)
                .valor(new BigDecimal("30.00"))
                .utilizado(false)
                .build());

        // RN0035 - cupom de troca pequeno, do Joao: combinado com um
        // cartao, empurra o valor do cartao para baixo de R$ 10,00 numa
        // compra modesta.
        cupomRepository.save(Cupom.builder()
                .codigo("TROCA-JOAO-PEQUENO")
                .tipo(TipoCupom.TROCA)
                .valor(new BigDecimal("20.00"))
                .utilizado(false)
                .cliente(joao)
                .build());

        // RN0036 - cupom de troca grande, do Joao: sozinho ja supera o
        // valor de uma compra pequena, para demonstrar a geracao do cupom
        // de troco pela diferenca.
        cupomRepository.save(Cupom.builder()
                .codigo("TROCA-JOAO-GRANDE")
                .tipo(TipoCupom.TROCA)
                .valor(new BigDecimal("500.00"))
                .utilizado(false)
                .cliente(joao)
                .build());
    }
}
