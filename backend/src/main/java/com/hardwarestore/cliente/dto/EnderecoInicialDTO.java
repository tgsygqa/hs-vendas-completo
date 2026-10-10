package com.hardwarestore.cliente.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RF0021 - todo cadastro de cliente exige um endereco residencial inicial.
 * RN0021/RN0022 - este endereco inicial sempre cobre cobranca e entrega ao
 * mesmo tempo (forcado no Service), por isso este DTO nao expõe esses campos.
 * RN0023 - composicao obrigatoria; observacoes e o unico campo opcional.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnderecoInicialDTO {

    @NotBlank(message = "O apelido do endereco e obrigatorio.")
    private String apelido;

    @NotBlank(message = "O tipo de residencia e obrigatorio.")
    private String tipoResidencia;

    @NotBlank(message = "O tipo de logradouro e obrigatorio.")
    private String tipoLogradouro;

    @NotBlank(message = "O logradouro e obrigatorio.")
    private String logradouro;

    @NotBlank(message = "O numero e obrigatorio.")
    private String numero;

    @NotBlank(message = "O bairro e obrigatorio.")
    private String bairro;

    @NotBlank(message = "O CEP e obrigatorio.")
    private String cep;

    @NotBlank(message = "A cidade e obrigatoria.")
    private String cidade;

    @NotBlank(message = "O estado e obrigatorio.")
    private String estado;

    @NotBlank(message = "O pais e obrigatorio.")
    private String pais;

    // RN0023 - unico campo opcional da composicao do endereco
    private String observacoes;
}
