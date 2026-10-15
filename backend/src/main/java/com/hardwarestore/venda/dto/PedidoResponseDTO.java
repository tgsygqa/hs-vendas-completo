package com.hardwarestore.venda.dto;

import com.hardwarestore.venda.model.Pedido;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PedidoResponseDTO {

    private Long id;
    private Long clienteId;
    private String clienteNome;
    private String status;
    private List<ItemPedidoResponseDTO> itens;
    private String enderecoResumo;
    private BigDecimal frete;
    private BigDecimal valorTotal;
    private LocalDateTime dataCriacao;
    private List<PagamentoResponseDTO> pagamentos;
    private List<CupomResponseDTO> cuponsUtilizados;
    // RN0038 - motivo devolvido pela "operadora" quando o pagamento e reprovado.
    private String motivoReprovacao;
    // RF0041-RF0045 - historico das solicitacoes de troca deste pedido.
    private List<SolicitacaoTrocaResponseDTO> solicitacoesTroca;

    public static PedidoResponseDTO fromEntity(Pedido p) {
        List<ItemPedidoResponseDTO> itens = p.getItens().stream()
                .map(ItemPedidoResponseDTO::fromEntity)
                .toList();
        List<PagamentoResponseDTO> pagamentos = p.getPagamentos().stream()
                .map(PagamentoResponseDTO::fromEntity)
                .toList();
        List<CupomResponseDTO> cupons = p.getCuponsUtilizados().stream()
                .map(CupomResponseDTO::fromEntity)
                .toList();
        List<SolicitacaoTrocaResponseDTO> trocas = p.getSolicitacoesTroca().stream()
                .map(SolicitacaoTrocaResponseDTO::fromEntity)
                .toList();
        String enderecoResumo = p.getEnderecoEntrega().getTipoLogradouro() + " "
                + p.getEnderecoEntrega().getLogradouro() + ", " + p.getEnderecoEntrega().getNumero()
                + " - " + p.getEnderecoEntrega().getCidade() + "/" + p.getEnderecoEntrega().getEstado();
        return new PedidoResponseDTO(
                p.getId(),
                p.getCliente().getId(),
                p.getCliente().getNome(),
                p.getStatus().name(),
                itens,
                enderecoResumo,
                p.getFrete(),
                p.getValorTotal(),
                p.getDataCriacao(),
                pagamentos,
                cupons,
                p.getMotivoReprovacao(),
                trocas
        );
    }
}

