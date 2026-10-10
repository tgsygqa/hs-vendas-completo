package com.hardwarestore.cliente.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
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

/**
 * RF0026 - um cliente pode ter varios enderecos, cada um identificado por um
 * apelido (frase curta). RN0021/RN0022 - cada endereco pode servir para
 * cobranca e/ou entrega; o cliente precisa manter ao menos um de cada.
 * RN0023 - composicao obrigatoria dos campos, com observacoes opcional.
 */
@Entity
@Table(name = "enderecos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // RF0026 - apelido / frase curta que identifica o endereco
    @Column(nullable = false)
    private String apelido;

    // RN0023 - composicao obrigatoria do endereco
    @Column(nullable = false)
    private String tipoResidencia;

    @Column(nullable = false)
    private String tipoLogradouro;

    @Column(nullable = false)
    private String logradouro;

    @Column(nullable = false)
    private String numero;

    @Column(nullable = false)
    private String bairro;

    @Column(nullable = false)
    private String cep;

    @Column(nullable = false)
    private String cidade;

    @Column(nullable = false)
    private String estado;

    @Column(nullable = false)
    private String pais;

    // RN0023 - campo opcional
    @Column
    private String observacoes;

    // RN0021 - este endereco serve como endereco de cobranca
    @Column(nullable = false)
    private boolean cobranca;

    // RN0022 - este endereco serve como endereco de entrega
    @Column(nullable = false)
    private boolean entrega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonBackReference
    private Cliente cliente;
}
