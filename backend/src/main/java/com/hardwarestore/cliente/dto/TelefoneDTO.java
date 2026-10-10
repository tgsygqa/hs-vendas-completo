package com.hardwarestore.cliente.dto;

import com.hardwarestore.cliente.model.TipoTelefone;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RN0026 - telefone composto de Tipo + DDD + Numero.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TelefoneDTO {

    @NotNull(message = "O tipo de telefone e obrigatorio.")
    private TipoTelefone tipo;

    @NotNull(message = "O DDD e obrigatorio.")
    @Pattern(regexp = "\\d{2}", message = "DDD invalido. Utilize 2 digitos.")
    private String ddd;

    @NotNull(message = "O numero de telefone e obrigatorio.")
    @Pattern(regexp = "\\d{4,5}-\\d{4}", message = "Numero de telefone invalido. Utilize o formato 98765-4321.")
    private String numero;
}
