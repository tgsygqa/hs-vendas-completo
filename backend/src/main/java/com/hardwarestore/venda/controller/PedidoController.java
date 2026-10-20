package com.hardwarestore.venda.controller;

import com.hardwarestore.venda.dto.FinalizarCompraRequestDTO;
import com.hardwarestore.venda.dto.FreteResponseDTO;
import com.hardwarestore.venda.dto.PedidoResponseDTO;
import com.hardwarestore.venda.dto.SolicitacaoTrocaResponseDTO;
import com.hardwarestore.venda.dto.SolicitarTrocaRequestDTO;
import com.hardwarestore.venda.model.StatusPedido;
import com.hardwarestore.venda.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CDU02 - Gerenciar Venda Eletronica. A criacao de um pedido e a listagem
 * "meus pedidos" pertencem ao cliente (por isso aninhadas sob
 * /api/clientes/{clienteId}/pedidos, igual carrinho); as acoes do
 * administrador (despachar, confirmar entrega) e a consulta geral vivem em
 * /api/pedidos. A validacao do pagamento (RN0037/RN0038) e automatica: quem
 * faz e o PagamentoValidacaoJob, nao ha endpoint para isso. Nao ha autenticacao no projeto (fora
 * do escopo do DRS): "administrador" e "cliente" sao so telas diferentes.
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

    // RF0034 - frete do carrinho atual do cliente para o endereco escolhido -> 200 OK.
    @GetMapping("/api/clientes/{clienteId}/frete")
    public ResponseEntity<FreteResponseDTO> calcularFrete(
            @PathVariable Long clienteId, @RequestParam Long enderecoId
    ) {
        return ResponseEntity.ok(pedidoService.calcularFrete(clienteId, enderecoId));
    }

    // "Meus pedidos" do cliente -> 200 OK
    @GetMapping("/api/clientes/{clienteId}/pedidos")
    public ResponseEntity<List<PedidoResponseDTO>> listarDoCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(pedidoService.listarDoCliente(clienteId));
    }

    // Painel do administrador; ?status=EM_PROCESSAMENTO (etc.) filtra.
    @GetMapping("/api/pedidos")
    public ResponseEntity<List<PedidoResponseDTO>> listarTodos(@RequestParam(required = false) StatusPedido status) {
        return ResponseEntity.ok(pedidoService.listarTodos(status));
    }

    @GetMapping("/api/pedidos/{id}")
    public ResponseEntity<PedidoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.buscarPorIdDTO(id));
    }

    // RF0039/RN0039 - administrador despacha: APROVADA -> EM_TRANSPORTE.
    @PatchMapping("/api/pedidos/{id}/despachar")
    public ResponseEntity<PedidoResponseDTO> despachar(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.despachar(id));
    }

    // RF0040/RN0040 - administrador confirma entrega: EM_TRANSPORTE -> ENTREGUE.
    @PatchMapping("/api/pedidos/{id}/confirmar-entrega")
    public ResponseEntity<PedidoResponseDTO> confirmarEntrega(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.confirmarEntrega(id));
    }

    // RF0041/RN0041/RN0043 - o cliente solicita a troca de itens de um pedido
    // ENTREGUE -> 201 Created, pedido passa a EM_TROCA.
    @PostMapping("/api/pedidos/{id}/trocas")
    public ResponseEntity<SolicitacaoTrocaResponseDTO> solicitarTroca(
            @PathVariable Long id, @RequestBody @Valid SolicitarTrocaRequestDTO dto
    ) {
        return ResponseEntity.status(201).body(pedidoService.solicitarTroca(id, dto));
    }

    @GetMapping("/api/pedidos/{id}/trocas")
    public ResponseEntity<List<SolicitacaoTrocaResponseDTO>> listarTrocasDoPedido(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.listarSolicitacoesTroca(id));
    }
}
