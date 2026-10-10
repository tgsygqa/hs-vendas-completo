package com.hardwarestore.cliente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RF0027 - cadastro de um cartao de credito para um cliente ja existente.
 * RN0024 - composicao obrigatoria: numero, nome impresso, bandeira, codigo
 * de seguranca. RN0025 - bandeira validada contra lista fechada no Service
 * (recebida aqui como String para permitir mensagem de erro amigavel em vez
 * do erro generico de deserializacao de enum do Jackson).
 *
 * O numero completo e o CVV chegam neste DTO apenas para validacao - nunca
 * sao persistidos (ver ClienteService#adicionarCartao).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartaoRequestDTO {

    @NotBlank(message = "O numero do cartao e obrigatorio.")
    @Pattern(regexp = "\\d{13,19}", message = "Numero de cartao invalido.")
    private String numero;

    @NotBlank(message = "A bandeira e obrigatoria.")
    private String bandeira;

    @NotBlank(message = "O nome impresso no cartao e obrigatorio.")
    private String titular;

    @NotBlank(message = "O codigo de seguranca e obrigatorio.")
    @Pattern(regexp = "\\d{3,4}", message = "Codigo de seguranca invalido.")
    private String cvv;
}
