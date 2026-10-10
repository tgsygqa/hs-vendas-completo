package com.hardwarestore.cliente.dto;

import com.hardwarestore.cliente.model.Genero;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

/**
 * RF0022 - Alterar dados cadastrais do cliente. Nao contem senha (RF0028 e
 * um fluxo isolado) nem enderecos/cartoes (RNF0034 - alteraveis a parte,
 * sem precisar editar o restante do cadastro).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClienteAlteracaoRequestDTO {

    @NotBlank(message = "O nome e obrigatorio.")
    private String nome;

    @NotBlank(message = "O CPF e obrigatorio.")
    @CPF(message = "CPF invalido.")
    @Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}", message = "CPF deve estar no formato 000.000.000-00.")
    private String cpf;

    @NotNull(message = "O genero e obrigatorio.")
    private Genero genero;

    @NotNull(message = "A data de nascimento e obrigatoria.")
    @Past(message = "A data de nascimento deve estar no passado.")
    private LocalDate dataNascimento;

    @NotNull(message = "O telefone e obrigatorio.")
    @Valid
    private TelefoneDTO telefone;

    @NotBlank(message = "O e-mail e obrigatorio.")
    @Email(message = "E-mail invalido.")
    private String email;
}
