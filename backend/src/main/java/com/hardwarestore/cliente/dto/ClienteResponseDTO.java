package com.hardwarestore.cliente.dto;

import com.hardwarestore.cliente.model.Cliente;
import com.hardwarestore.cliente.model.Genero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de saida da API. Propositalmente NAO possui o campo senha - garante
 * que o hash da senha nunca seja exposto em nenhuma resposta.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ClienteResponseDTO {

    private Long id;
    private String nome;
    private String cpf;
    private Genero genero;
    private LocalDate dataNascimento;
    private TelefoneDTO telefone;
    private String email;
    private boolean ativo;
    private Integer ranking;
    private List<EnderecoResponseDTO> enderecos;
    private List<CartaoResponseDTO> cartoes;

    public static ClienteResponseDTO fromEntity(Cliente c) {
        TelefoneDTO telefoneDTO = c.getTelefone() == null ? null : new TelefoneDTO(
                c.getTelefone().getTipo(), c.getTelefone().getDdd(), c.getTelefone().getNumero()
        );
        List<EnderecoResponseDTO> enderecos = c.getEnderecos().stream()
                .map(EnderecoResponseDTO::fromEntity).toList();
        List<CartaoResponseDTO> cartoes = c.getCartoes().stream()
                .map(CartaoResponseDTO::fromEntity).toList();

        return new ClienteResponseDTO(
                c.getId(), c.getNome(), c.getCpf(), c.getGenero(), c.getDataNascimento(),
                telefoneDTO, c.getEmail(), c.isAtivo(), c.getRanking(), enderecos, cartoes
        );
    }

    /** Versao resumida, sem enderecos/cartoes - usada na listagem (RF0024). */
    public static ClienteResponseDTO resumo(Cliente c) {
        TelefoneDTO telefoneDTO = c.getTelefone() == null ? null : new TelefoneDTO(
                c.getTelefone().getTipo(), c.getTelefone().getDdd(), c.getTelefone().getNumero()
        );
        return new ClienteResponseDTO(
                c.getId(), c.getNome(), c.getCpf(), c.getGenero(), c.getDataNascimento(),
                telefoneDTO, c.getEmail(), c.isAtivo(), c.getRanking(), null, null
        );
    }
}
