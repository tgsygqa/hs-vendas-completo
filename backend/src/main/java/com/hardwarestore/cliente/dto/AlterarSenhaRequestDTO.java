package com.hardwarestore.cliente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RF0028 - alteracao de senha isolada, sem precisar editar o restante do
 * cadastro. RNF0031 - senha forte. RNF0032 - confirmacao (verificada no
 * Service). RNF0033 - a nova senha e persistida como hash.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlterarSenhaRequestDTO {

    @NotBlank(message = "A nova senha e obrigatoria.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\W).{8,}$",
            message = "Senha fraca. Deve ter 8 caracteres, letras maiusculas, minusculas e simbolo."
    )
    private String novaSenha;

    @NotBlank(message = "A confirmacao da nova senha e obrigatoria.")
    private String confirmacaoSenha;
}
