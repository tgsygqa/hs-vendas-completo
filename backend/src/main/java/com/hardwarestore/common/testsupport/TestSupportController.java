package com.hardwarestore.common.testsupport;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints de apoio aos testes Cypress. Ver TestSupportService - so
 * existem com app.test-support.enabled=true.
 */
@RestController
@RequestMapping("/api/test-support")
@ConditionalOnProperty(name = "app.test-support.enabled", havingValue = "true")
public class TestSupportController {

    private final TestSupportService service;

    public TestSupportController(TestSupportService service) {
        this.service = service;
    }

    // Volta a base ao estado inicial dos seeders.
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetar() {
        service.resetar();
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // Define o estoque de um produto (ex.: simular que outro cliente comprou).
    @PutMapping("/produtos/{produtoId}/estoque")
    public ResponseEntity<Map<String, String>> definirEstoque(
            @PathVariable Long produtoId, @RequestParam int quantidade
    ) {
        service.definirEstoque(produtoId, quantidade);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // Ajusta o prazo de bloqueio do carrinho do cliente para agora + N segundos
    // (use um valor negativo para deixa-lo ja vencido).
    @PutMapping("/carrinhos/{clienteId}/bloqueio")
    public ResponseEntity<Map<String, String>> definirBloqueio(
            @PathVariable Long clienteId, @RequestParam long segundos
    ) {
        service.definirBloqueio(clienteId, segundos);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // Executa agora a varredura que libera os itens de carrinho vencidos.
    @PostMapping("/carrinhos/varrer")
    public ResponseEntity<Map<String, Integer>> varrer() {
        return ResponseEntity.ok(Map.of("itensLiberados", service.varrerExpirados()));
    }
}
