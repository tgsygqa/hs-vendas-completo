package com.hardwarestore.venda.controller;

import com.hardwarestore.venda.dto.ConfirmarRecebimentoTrocaRequestDTO;
import com.hardwarestore.venda.dto.SolicitacaoTrocaResponseDTO;
import com.hardwarestore.venda.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Acoes do administrador sobre as solicitacoes de troca (RF0042 a RF0045).
 */
@RestController
@RequestMapping("/api/trocas")
public class TrocaController {

    private final PedidoService pedidoService;

    public TrocaController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // RF0043 - o administrador visualiza todas as solicitacoes de troca.
    @GetMapping
    public ResponseEntity<List<SolicitacaoTrocaResponseDTO>> listar() {
        return ResponseEntity.ok(pedidoService.listarTrocas());
    }

    // RF0042 - autorizar: pedido EM_TROCA -> TROCA_AUTORIZADA (e RN0046:
    // notifica o cliente).
    @PatchMapping("/{id}/autorizar")
    public ResponseEntity<SolicitacaoTrocaResponseDTO> autorizar(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.autorizarTroca(id));
    }

    // RF0044/RF0054/RN0042/RF0045 - confirma o recebimento, informa se os
    // itens voltam ao estoque, pedido -> TROCADO e gera o cupom de troca.
    @PatchMapping("/{id}/confirmar-recebimento")
    public ResponseEntity<SolicitacaoTrocaResponseDTO> confirmarRecebimento(
            @PathVariable Long id, @RequestBody @Valid ConfirmarRecebimentoTrocaRequestDTO dto
    ) {
        return ResponseEntity.ok(pedidoService.confirmarRecebimentoTroca(id, dto));
    }
}
