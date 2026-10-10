package com.hardwarestore.cliente.dto;

import com.hardwarestore.cliente.model.Endereco;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EnderecoResponseDTO {

    private Long id;
    private String apelido;
    private String tipoResidencia;
    private String tipoLogradouro;
    private String logradouro;
    private String numero;
    private String bairro;
    private String cep;
    private String cidade;
    private String estado;
    private String pais;
    private String observacoes;
    private boolean cobranca;
    private boolean entrega;

    public static EnderecoResponseDTO fromEntity(Endereco e) {
        return new EnderecoResponseDTO(
                e.getId(), e.getApelido(), e.getTipoResidencia(), e.getTipoLogradouro(),
                e.getLogradouro(), e.getNumero(), e.getBairro(), e.getCep(), e.getCidade(),
                e.getEstado(), e.getPais(), e.getObservacoes(), e.isCobranca(), e.isEntrega()
        );
    }
}
