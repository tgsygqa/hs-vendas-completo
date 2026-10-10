package com.hardwarestore.cliente.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RF0026 - cadastro de um endereco adicional para um cliente ja existente.
 * RN0021/RN0022 - o endereco precisa ser marcado como cobranca e/ou entrega
 * (validado no Service; ao menos um dos dois deve ser verdadeiro).
 * RN0023 - composicao obrigatoria; observacoes e opcional.
 * RNF0034 - este cadastro e independente da alteracao dos demais dados do
 * cliente (endpoint proprio, nao mexe em nome/cpf/telefone/email).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnderecoRequestDTO {

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

    // RN0021/RN0022 - ao menos um dos dois deve ser verdadeiro (validado no Service)
    private boolean cobranca;

    private boolean entrega;
}
