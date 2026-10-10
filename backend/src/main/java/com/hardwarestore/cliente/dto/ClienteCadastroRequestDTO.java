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
 * RF0021 - Cadastrar cliente. RN0026 - dados obrigatorios (Genero, Nome,
 * Data de Nascimento, CPF, Telefone, e-mail, senha, endereco residencial).
 * RNF0031 - senha forte. RNF0032 - confirmacao de senha (verificada no
 * Service, pois e uma validacao cruzada entre dois campos).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClienteCadastroRequestDTO {

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

    // RNF0031 - senha forte: minimo 8 caracteres, 1 maiuscula, 1 minuscula, 1 simbolo
    @NotBlank(message = "A senha e obrigatoria.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\W).{8,}$",
            message = "Senha fraca. Deve ter 8 caracteres, letras maiusculas, minusculas e simbolo."
    )
    private String senha;

    // RNF0032 - confirmacao de senha (comparada com "senha" no Service)
    @NotBlank(message = "A confirmacao de senha e obrigatoria.")
    private String confirmacaoSenha;

    // RF0021 / RN0021 / RN0022 - endereco residencial inicial, obrigatorio
    @NotNull(message = "O endereco residencial inicial e obrigatorio.")
    @Valid
    private EnderecoInicialDTO endereco;
}
