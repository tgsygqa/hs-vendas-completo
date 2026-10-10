package com.hardwarestore.venda.controller;

import com.hardwarestore.venda.dto.FinalizarCompraRequestDTO;
import com.hardwarestore.venda.dto.PedidoResponseDTO;
import com.hardwarestore.venda.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * A criacao de um pedido pertence ao cliente que compra (por isso
 * aninhada sob /api/clientes/{clienteId}/pedidos, igual carrinho); a
 * listagem para o painel do administrador vive em /api/pedidos.
 *
 * Nao ha endpoints de despachar/confirmar-entrega nesta entrega: RF0039 e
 * RF0040 estao explicitamente fora do escopo da atividade de 06/10
 * (instrucoes do professor) e ficam para 20/10, quando o caso de uso
 * completo for apresentado.
 */
@RestController
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // RF0033/RF0035/RF0036/RF0037/RF0038 - finalizar compra -> 201
    // Created, com o pedido em EM_PROCESSAMENTO.
    @PostMapping("/api/clientes/{clienteId}/pedidos")
    public ResponseEntity<PedidoResponseDTO> finalizarCompra(
            @PathVariable Long clienteId, @RequestBody @Valid FinalizarCompraRequestDTO dto
    ) {
        PedidoResponseDTO pedido = pedidoService.finalizarCompra(
                clienteId, dto.getEnderecoId(), dto.getCartoes(), dto.getCupomIds());
        return ResponseEntity.status(201).body(pedido);
    }

    @GetMapping("/api/pedidos")
    public ResponseEntity<List<PedidoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    @GetMapping("/api/pedidos/{id}")
    public ResponseEntity<PedidoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.buscarPorIdDTO(id));
    }
}
