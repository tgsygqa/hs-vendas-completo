package com.hardwarestore.cliente.controller;

import com.hardwarestore.cliente.dto.AlterarSenhaRequestDTO;
import com.hardwarestore.cliente.dto.CartaoRequestDTO;
import com.hardwarestore.cliente.dto.CartaoResponseDTO;
import com.hardwarestore.cliente.dto.ClienteAlteracaoRequestDTO;
import com.hardwarestore.cliente.dto.ClienteCadastroRequestDTO;
import com.hardwarestore.cliente.dto.ClienteResponseDTO;
import com.hardwarestore.cliente.dto.EnderecoRequestDTO;
import com.hardwarestore.cliente.dto.EnderecoResponseDTO;
import com.hardwarestore.cliente.dto.TransacaoResponseDTO;
import com.hardwarestore.cliente.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * API REST do modulo de Gestao de Clientes (DRS_LES_2_2026, grupo Cadastro
 * de Clientes). Nenhuma regra de negocio fica aqui - o controller apenas
 * recebe a requisicao, delega ao ClienteService e traduz o retorno para o
 * status HTTP adequado. Erros sao tratados de forma centralizada pelo
 * GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // ------------------------------------------------------------------
    // RF0021 - Cadastrar cliente -> 201 Created
    // ------------------------------------------------------------------
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> cadastrar(@RequestBody @Valid ClienteCadastroRequestDTO dto) {
        ClienteResponseDTO criado = clienteService.cadastrar(dto);
        URI location = URI.create("/api/clientes/" + criado.getId());
        return ResponseEntity.created(location).body(criado);
    }

    // ------------------------------------------------------------------
    // RF0024 - Consultar clientes com filtros combinaveis/isolados -> 200 OK
    // ------------------------------------------------------------------
    @GetMapping
    public ResponseEntity<List<ClienteResponseDTO>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Boolean ativo
    ) {
        return ResponseEntity.ok(clienteService.listar(nome, cpf, email, ativo));
    }

    // RF0024 - Consultar cliente por ID (dados completos, incl. enderecos/cartoes)
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    // ------------------------------------------------------------------
    // RF0022 - Alterar dados cadastrais -> 200 OK / 400 / 404 / 409
    // ------------------------------------------------------------------
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> alterar(
            @PathVariable Long id, @RequestBody @Valid ClienteAlteracaoRequestDTO dto
    ) {
        return ResponseEntity.ok(clienteService.alterar(id, dto));
    }

    // ------------------------------------------------------------------
    // RF0023 - Inativar / Reativar (soft delete) -> 200 OK / 404
    // ------------------------------------------------------------------
    @PatchMapping("/{id}/inativar")
    public ResponseEntity<ClienteResponseDTO> inativar(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.inativar(id));
    }

    @PatchMapping("/{id}/reativar")
    public ResponseEntity<ClienteResponseDTO> reativar(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.reativar(id));
    }

    // ------------------------------------------------------------------
    // Exclusao definitiva -> 204 No Content / 404 / 409 (cliente com transacoes)
    // ------------------------------------------------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        clienteService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    // RF0025 - Consulta de todas as transacoes do cliente -> 200 OK / 404
    // ------------------------------------------------------------------
    @GetMapping("/{id}/transacoes")
    public ResponseEntity<List<TransacaoResponseDTO>> consultarTransacoes(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.consultarTransacoes(id));
    }

    // ------------------------------------------------------------------
    // RF0026 - Enderecos (RN0021/RN0022/RN0023, RNF0034)
    // ------------------------------------------------------------------
    @PostMapping("/{id}/enderecos")
    public ResponseEntity<EnderecoResponseDTO> adicionarEndereco(
            @PathVariable Long id, @RequestBody @Valid EnderecoRequestDTO dto
    ) {
        EnderecoResponseDTO criado = clienteService.adicionarEndereco(id, dto);
        return ResponseEntity.status(201).body(criado);
    }

    @DeleteMapping("/{id}/enderecos/{enderecoId}")
    public ResponseEntity<Void> removerEndereco(@PathVariable Long id, @PathVariable Long enderecoId) {
        clienteService.removerEndereco(id, enderecoId);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    // RF0027 - Cartoes de credito (RN0024/RN0025)
    // ------------------------------------------------------------------
    @PostMapping("/{id}/cartoes")
    public ResponseEntity<CartaoResponseDTO> adicionarCartao(
            @PathVariable Long id, @RequestBody @Valid CartaoRequestDTO dto
    ) {
        CartaoResponseDTO criado = clienteService.adicionarCartao(id, dto);
        return ResponseEntity.status(201).body(criado);
    }

    @PatchMapping("/{id}/cartoes/{cartaoId}/preferencial")
    public ResponseEntity<CartaoResponseDTO> tornarCartaoPreferencial(
            @PathVariable Long id, @PathVariable Long cartaoId
    ) {
        return ResponseEntity.ok(clienteService.tornarCartaoPreferencial(id, cartaoId));
    }

    @DeleteMapping("/{id}/cartoes/{cartaoId}")
    public ResponseEntity<Void> removerCartao(@PathVariable Long id, @PathVariable Long cartaoId) {
        clienteService.removerCartao(id, cartaoId);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    // RF0028 - Alteracao de senha isolada (RNF0031/RNF0032/RNF0033)
    // ------------------------------------------------------------------
    @PatchMapping("/{id}/senha")
    public ResponseEntity<Void> alterarSenha(@PathVariable Long id, @RequestBody @Valid AlterarSenhaRequestDTO dto) {
        clienteService.alterarSenha(id, dto);
        return ResponseEntity.noContent().build();
    }
}
