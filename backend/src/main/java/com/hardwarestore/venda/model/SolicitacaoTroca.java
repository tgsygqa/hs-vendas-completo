package com.hardwarestore.venda.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * RF0041 a RF0045 - o processo de troca inteiro. Guarda os detalhes (quais
 * itens, quanto, por que, a decisao do administrador); QUEM manda no
 * estado do pedido continua sendo Pedido.status (RN0041: "o status do
 * pedido devera ser EM TROCA") - esta classe nunca duplica esse estado,
 * so acrescenta o que falta nele.
 *
 * Escopo (enunciado da atividade): estorno financeiro no cartao e recusa de
 * troca pelo administrador NAO fazem parte dos requisitos e nao sao
 * implementados - toda solicitacao e autorizada e depois recebida.
 */
@Entity
@Table(name = "solicitacoes_troca")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitacaoTroca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    @JsonBackReference
    private Pedido pedido;

    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<ItemSolicitacaoTroca> itens = new ArrayList<>();

    // RF0041 - o cliente informa o motivo ao solicitar.
    @Column(nullable = false, length = 1000)
    private String motivoCliente;

    @Column(nullable = false)
    private LocalDateTime dataSolicitacao;

    // Preenchido quando o administrador autoriza (RF0042).
    private LocalDateTime dataDecisaoAdmin;

    // RF0044 - "o administrador devera informar se os itens trocados
    // deverao retornar ao estoque". Nulo ate a confirmacao de recebimento;
    // true/false a partir dai.
    private Boolean itensRetornaramEstoque;

    private LocalDateTime dataRecebimento;

    // RF0045 - o cupom de troca gerado para o cliente apos a confirmacao
    // do recebimento. Nulo ate TROCADO.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cupom_gerado_id")
    private Cupom cupomGerado;
}
