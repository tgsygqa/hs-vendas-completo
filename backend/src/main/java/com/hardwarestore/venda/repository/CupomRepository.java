package com.hardwarestore.venda.repository;

import com.hardwarestore.venda.model.Cupom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CupomRepository extends JpaRepository<Cupom, Long> {

    // Cupons que o cliente pode usar agora: os dele (cupons de troca,
    // RF0045 - fora de escopo gerar, mas pre-carregados) mais os
    // promocionais de uso geral (cliente nulo), que ainda nao foram
    // usados. Usado pela tela de pagamento para listar as opcoes.
    @Query("SELECT c FROM Cupom c WHERE c.utilizado = false "
            + "AND (c.cliente.id = :clienteId OR c.cliente IS NULL)")
    List<Cupom> buscarDisponiveisParaCliente(@Param("clienteId") Long clienteId);
}
