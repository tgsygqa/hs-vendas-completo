package com.hardwarestore.venda.model;

import com.hardwarestore.cliente.model.Cliente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * RN0046 - "quando o administrador autorizar uma troca o sistema devera
 * gerar uma notificacao sobre tal ao cliente". Tambem usada para avisar o
 * cliente da troca autorizada e do cupom de troca gerado (RF0045), que o
 * DRS pede que seja "disponibilizado para o cliente".
 */
@Entity
@Table(name = "notificacoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 1000)
    private String mensagem;

    @Column(nullable = false)
    private LocalDateTime criadaEm;

    @Column(nullable = false)
    @Builder.Default
    private boolean lida = false;
}
