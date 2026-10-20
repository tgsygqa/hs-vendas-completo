package com.hardwarestore.venda.service;

import com.hardwarestore.cliente.model.Cliente;
import com.hardwarestore.cliente.repository.ClienteRepository;
import com.hardwarestore.common.exception.RecursoNaoEncontradoException;
import com.hardwarestore.venda.dto.NotificacaoResponseDTO;
import com.hardwarestore.venda.model.Notificacao;
import com.hardwarestore.venda.repository.NotificacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RN0046 - gera e consulta as notificacoes do cliente (autorizacao de
 * troca, cupom de troca disponibilizado).
 */
@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final ClienteRepository clienteRepository;

    public NotificacaoService(NotificacaoRepository notificacaoRepository,
                              ClienteRepository clienteRepository) {
        this.notificacaoRepository = notificacaoRepository;
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public void notificar(Cliente cliente, String mensagem) {
        notificacaoRepository.save(Notificacao.builder()
                .cliente(cliente)
                .mensagem(mensagem)
                .criadaEm(LocalDateTime.now())
                .build());
    }

    public List<NotificacaoResponseDTO> listar(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new RecursoNaoEncontradoException("Cliente não encontrado. ID: " + clienteId);
        }
        return notificacaoRepository.findByClienteIdOrderByCriadaEmDescIdDesc(clienteId).stream()
                .map(NotificacaoResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public NotificacaoResponseDTO marcarComoLida(Long clienteId, Long notificacaoId) {
        Notificacao notificacao = notificacaoRepository.findById(notificacaoId)
                .filter(n -> n.getCliente().getId().equals(clienteId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada. ID: " + notificacaoId));
        notificacao.setLida(true);
        return NotificacaoResponseDTO.fromEntity(notificacaoRepository.save(notificacao));
    }
}
