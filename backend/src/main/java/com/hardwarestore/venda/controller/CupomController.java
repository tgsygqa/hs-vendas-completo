package com.hardwarestore.venda.controller;

import com.hardwarestore.venda.dto.CupomResponseDTO;
import com.hardwarestore.venda.service.CupomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * RF0037 - lista os cupons (promocionais de uso geral + de troca do
 * proprio cliente) que ainda podem ser usados, para a tela de pagamento
 * oferecer como opcao.
 */
@RestController
@RequestMapping("/api/clientes/{clienteId}/cupons")
public class CupomController {

    private final CupomService cupomService;

    public CupomController(CupomService cupomService) {
        this.cupomService = cupomService;
    }

    @GetMapping
    public ResponseEntity<List<CupomResponseDTO>> listarDisponiveis(@PathVariable Long clienteId) {
        return ResponseEntity.ok(cupomService.listarDisponiveisParaCliente(clienteId));
    }
}
