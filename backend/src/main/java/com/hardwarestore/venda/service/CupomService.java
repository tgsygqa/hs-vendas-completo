package com.hardwarestore.venda.service;

import com.hardwarestore.venda.dto.CupomResponseDTO;
import com.hardwarestore.venda.repository.CupomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * So consulta os cupons que o cliente pode usar. Quem GERA cupons e o
 * PedidoService: o cupom de troco (RN0036), na finalizacao da compra, e o
 * cupom de troca (RF0045), na confirmacao do recebimento dos itens devolvidos.
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
