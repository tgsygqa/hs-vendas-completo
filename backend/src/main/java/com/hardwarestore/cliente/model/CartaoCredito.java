package com.hardwarestore.cliente.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * RF0027 - um cliente pode ter varios cartoes de credito, um deles marcado
 * como preferencial. RN0024 - composicao obrigatoria (numero, nome impresso,
 * bandeira, codigo de seguranca). RN0025 - bandeira de lista fechada.
 *
 * O numero completo do cartao e o codigo de seguranca (CVV) NUNCA sao
 * persistidos - apenas os 4 ultimos digitos do numero sao armazenados, pelo
 * mesmo principio de protecao de dados sensiveis usado no front (V08.html).
 */
@Entity
@Table(name = "cartoes_credito")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartaoCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // RN0025 - bandeira deve ser de uma lista fechada registrada no sistema
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Bandeira bandeira;

    // RN0024 - apenas os 4 ultimos digitos do numero sao armazenados
    @Column(nullable = false, length = 4)
    private String ultimosQuatroDigitos;

    // RN0024 - nome impresso no cartao
    @Column(nullable = false)
    private String titular;

    // RF0027 - deve sempre haver um cartao preferencial quando ha cartoes
    @Column(nullable = false)
    private boolean preferencial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonBackReference
    private Cliente cliente;
}
