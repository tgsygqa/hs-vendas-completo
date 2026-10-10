package com.hardwarestore.venda.config;

import com.hardwarestore.venda.model.Produto;
import com.hardwarestore.venda.repository.ProdutoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Semeia produtos de exemplo para o carrinho e a venda terem o que vender.
 * Os mesmos produtos (nomes, categorias, fornecedor) ja usados nos
 * wireframes do CDU02, para manter a demonstracao consistente com o que o
 * professor ja viu no documento.
 *
 * @Order(1) - roda antes do DataSeeder do modulo Cliente (que, a partir de
 * agora, usa estes produtos para criar pedidos de exemplo realistas).
 */
@Component
@Order(1)
public class ProdutoSeeder implements CommandLineRunner {

    private final ProdutoRepository produtoRepository;

    public ProdutoSeeder(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @Override
    public void run(String... args) {
        produtoRepository.save(Produto.builder()
                .nome("Placa de Video RTX 4090 24GB GDDR6X")
                .categoria("Placas de Video")
                .fornecedor("Hardware Store Oficial")
                .precoVenda(new BigDecimal("14500.00"))
                .quantidadeEstoque(12)
                .ativo(true)
                .build());

        produtoRepository.save(Produto.builder()
                .nome("Processador AMD Ryzen 9 7950X3D")
                .categoria("Processadores")
                .fornecedor("Hardware Store Oficial")
                .precoVenda(new BigDecimal("4899.90"))
                .quantidadeEstoque(25)
                .ativo(true)
                .build());

        produtoRepository.save(Produto.builder()
                .nome("Headset Gamer Pro X")
                .categoria("Perifericos")
                .fornecedor("Hardware Store Oficial")
                .precoVenda(new BigDecimal("349.90"))
                .quantidadeEstoque(40)
                .ativo(true)
                .build());

        produtoRepository.save(Produto.builder()
                .nome("Teclado Mecanico RGB 60%")
                .categoria("Perifericos")
                .fornecedor("Hardware Store Oficial")
                .precoVenda(new BigDecimal("459.90"))
                .quantidadeEstoque(30)
                .ativo(true)
                .build());

        produtoRepository.save(Produto.builder()
                .nome("SSD NVMe M.2 Samsung 990 PRO 2TB")
                .categoria("Armazenamento")
                .fornecedor("Hardware Store Oficial")
                .precoVenda(new BigDecimal("1250.00"))
                .quantidadeEstoque(18)
                .ativo(true)
                .build());
    }
}
