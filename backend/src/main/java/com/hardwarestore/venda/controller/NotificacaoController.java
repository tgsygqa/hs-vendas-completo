package com.hardwarestore.venda.controller;

import com.hardwarestore.venda.dto.NotificacaoResponseDTO;
import com.hardwarestore.venda.service.NotificacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** RN0046 - notificacoes do cliente (troca autorizada, cupom gerado). */
@RestController
@RequestMapping("/api/clientes/{clienteId}/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping
    public ResponseEntity<List<NotificacaoResponseDTO>> listar(@PathVariable Long clienteId) {
        return ResponseEntity.ok(notificacaoService.listar(clienteId));
    }

    @PatchMapping("/{notificacaoId}/lida")
    public ResponseEntity<NotificacaoResponseDTO> marcarComoLida(
            @PathVariable Long clienteId, @PathVariable Long notificacaoId
    ) {
        return ResponseEntity.ok(notificacaoService.marcarComoLida(clienteId, notificacaoId));
    }
}
