package com.hardwarestore.cliente.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade central do modulo de Gestao de Clientes.
 * Cobre RF0021 (Cadastrar), RF0022 (Alterar), RF0023 (Inativar/Reativar),
 * RF0024 (Consultar) e RF0025 (Consulta de transacoes - ver ClienteService).
 * RN0026 - dados obrigatorios do cliente. RN0027 - ranking numerico.
 * RNF0035 - codigo unico de cliente gerado pelo sistema (o proprio id serve
 * como esse codigo unico, ja que e gerado automaticamente pelo banco).
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // RN0026 - dados obrigatorios
    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true, length = 14)
    private String cpf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Genero genero;

    @Column(nullable = false)
    private LocalDate dataNascimento;

    // RN0026 - telefone composto de tipo + DDD + numero
    @Embedded
    private Telefone telefone;

    @Column(nullable = false, unique = true)
    private String email;

    // RNF0033 - a senha e sempre armazenada como hash (ver ClienteService)
    @Column(nullable = false)
    private String senha;

    // RF0023 - soft delete
    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    // RN0027 - ranking numerico do cliente com base no perfil de compra.
    // Neste modulo isolado de Clientes (sem integracao com Vendas), o
    // ranking nasce zerado e nao e recalculado automaticamente.
    @Column(nullable = false)
    @Builder.Default
    private Integer ranking = 0;

    // RF0026 - varios enderecos por cliente
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<Endereco> enderecos = new ArrayList<>();

    // RF0027 - varios cartoes por cliente
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<CartaoCredito> cartoes = new ArrayList<>();
}
