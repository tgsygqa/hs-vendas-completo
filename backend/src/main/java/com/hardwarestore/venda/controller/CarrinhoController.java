package com.hardwarestore.venda.controller;

import com.hardwarestore.venda.dto.AdicionarItemCarrinhoRequestDTO;
import com.hardwarestore.venda.dto.AlterarQuantidadeRequestDTO;
import com.hardwarestore.venda.dto.CarrinhoResponseDTO;
import com.hardwarestore.venda.service.CarrinhoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Aninhado sob /api/clientes/{clienteId}, no mesmo padrao ja usado para
 * enderecos e cartoes - o carrinho e sempre o de um cliente especifico.
 */
@RestController
@RequestMapping("/api/clientes/{clienteId}/carrinho")
public class CarrinhoController {

    private final CarrinhoService carrinhoService;

    public CarrinhoController(CarrinhoService carrinhoService) {
        this.carrinhoService = carrinhoService;
    }

    // RF0031 - exibir o carrinho atual -> 200 OK
    @GetMapping
    public ResponseEntity<CarrinhoResponseDTO> obter(@PathVariable Long clienteId) {
        return ResponseEntity.ok(carrinhoService.obterCarrinho(clienteId));
    }

    // RF0031/RN0031 - adicionar item -> 201 Created / 404 (cliente ou
    // produto inexistente) / 409 (estoque insuficiente)
    @PostMapping("/itens")
    public ResponseEntity<CarrinhoResponseDTO> adicionarItem(
            @PathVariable Long clienteId, @RequestBody @Valid AdicionarItemCarrinhoRequestDTO dto
    ) {
        CarrinhoResponseDTO carrinho = carrinhoService.adicionarItem(clienteId, dto.getProdutoId(), dto.getQuantidade());
        return ResponseEntity.status(201).body(carrinho);
    }

    // RF0032 - alterar quantidade de um item -> 200 OK / 404 / 409
    @PatchMapping("/itens/{itemId}")
    public ResponseEntity<CarrinhoResponseDTO> alterarQuantidade(
            @PathVariable Long clienteId, @PathVariable Long itemId, @RequestBody @Valid AlterarQuantidadeRequestDTO dto
    ) {
        return ResponseEntity.ok(carrinhoService.alterarQuantidade(clienteId, itemId, dto.getQuantidade()));
    }

    // RF0031 - remover item do carrinho -> 200 OK (devolve o carrinho
    // atualizado, mais util para o front do que 204 sem corpo aqui)
    @DeleteMapping("/itens/{itemId}")
    public ResponseEntity<CarrinhoResponseDTO> removerItem(@PathVariable Long clienteId, @PathVariable Long itemId) {
        return ResponseEntity.ok(carrinhoService.removerItem(clienteId, itemId));
    }

    // RNF0042 - devolve ao carrinho um item que tinha expirado -> 200 OK /
    // 409 (estoque insuficiente)
    @PostMapping("/expirados/{expiradoId}/readicionar")
    public ResponseEntity<CarrinhoResponseDTO> readicionarExpirado(
            @PathVariable Long clienteId, @PathVariable Long expiradoId
    ) {
        return ResponseEntity.ok(carrinhoService.readicionarExpirado(clienteId, expiradoId));
    }

    // RNF0042 - dispensa o aviso de item expirado sem readicionar -> 200 OK
    @DeleteMapping("/expirados/{expiradoId}")
    public ResponseEntity<CarrinhoResponseDTO> dispensarExpirado(
            @PathVariable Long clienteId, @PathVariable Long expiradoId
    ) {
        return ResponseEntity.ok(carrinhoService.dispensarExpirado(clienteId, expiradoId));
    }
}
