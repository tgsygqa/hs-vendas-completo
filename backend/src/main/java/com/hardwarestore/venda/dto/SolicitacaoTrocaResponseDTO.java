package com.hardwarestore.venda.dto;

import com.hardwarestore.venda.model.SolicitacaoTroca;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SolicitacaoTrocaResponseDTO {

    private Long id;
    private Long pedidoId;
    private Long clienteId;
    private String clienteNome;
    private List<ItemSolicitacaoTrocaResponseDTO> itens;
    private String motivoCliente;
    // Reflete Pedido.status no momento da consulta (EM_TROCA, TROCA_AUTORIZADA,
    // TROCADO, ou ENTREGUE se esta solicitacao foi recusada) - ver a
    // observacao na classe SolicitacaoTroca sobre nao duplicar esse estado.
    private String statusPedido;
    private LocalDateTime dataSolicitacao;
    private boolean recusada;
    private String motivoRecusa;
    private Boolean itensRetornaramEstoque;
    private CupomResponseDTO cupomGerado;

    public static SolicitacaoTrocaResponseDTO fromEntity(SolicitacaoTroca s) {
        List<ItemSolicitacaoTrocaResponseDTO> itens = s.getItens().stream()
                .map(ItemSolicitacaoTrocaResponseDTO::fromEntity)
                .toList();
        CupomResponseDTO cupom = s.getCupomGerado() != null ? CupomResponseDTO.fromEntity(s.getCupomGerado()) : null;
        return new SolicitacaoTrocaResponseDTO(
                s.getId(),
                s.getPedido().getId(),
                s.getPedido().getCliente().getId(),
                s.getPedido().getCliente().getNome(),
                itens,
                s.getMotivoCliente(),
                s.getPedido().getStatus().name(),
                s.getDataSolicitacao(),
                s.isRecusada(),
                s.getMotivoRecusa(),
                s.getItensRetornaramEstoque(),
                cupom
        );
    }
}
