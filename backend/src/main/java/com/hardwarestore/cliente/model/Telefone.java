package com.hardwarestore.cliente.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RN0026 - telefone do cliente composto de Tipo + DDD + Numero.
 * Embutido diretamente na tabela de clientes (nao e uma entidade propria).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Telefone {

    @Enumerated(EnumType.STRING)
    @Column(name = "telefone_tipo", nullable = false)
    private TipoTelefone tipo;

    @Column(name = "telefone_ddd", nullable = false, length = 2)
    private String ddd;

    @Column(name = "telefone_numero", nullable = false, length = 10)
    private String numero;
}
