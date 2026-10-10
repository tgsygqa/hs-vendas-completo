package com.hardwarestore.venda.service;

import com.hardwarestore.venda.dto.CupomResponseDTO;
import com.hardwarestore.venda.repository.CupomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * So consulta - a geracao de cupom de troca a partir de uma devolucao
 * (RF0045) esta fora de escopo; os cupons existem porque o CupomSeeder os
 * pre-carrega (instrucoes do professor autorizam isso explicitamente). O
 * cupom de troco gerado pela RN0036 nasce dentro do PedidoService, no
 * momento da compra.
 */
@Service
public class CupomService {

    private final CupomRepository cupomRepository;

    public CupomService(CupomRepository cupomRepository) {
        this.cupomRepository = cupomRepository;
    }

    public List<CupomResponseDTO> listarDisponiveisParaCliente(Long clienteId) {
        return cupomRepository.buscarDisponiveisParaCliente(clienteId).stream()
                .map(CupomResponseDTO::fromEntity)
                .toList();
    }
}
