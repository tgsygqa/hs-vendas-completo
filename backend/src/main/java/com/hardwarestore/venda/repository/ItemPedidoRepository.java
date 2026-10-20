package com.hardwarestore.venda.repository;

import com.hardwarestore.venda.model.ItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    // RN0028/RN0044 - quantidade deste produto que esta bloqueada (reservada)
    // em compras ainda EM PROCESSAMENTO, ou seja, aguardando a validacao do
    // pagamento. Se o pagamento for reprovado o bloqueio some e o item
    // continua em estoque; se for aprovado, o estoque fisico sofre a baixa.
    @Query("SELECT COALESCE(SUM(i.quantidade), 0L) FROM ItemPedido i "
            + "WHERE i.produto.id = :produtoId "
            + "AND i.pedido.status = com.hardwarestore.venda.model.StatusPedido.EM_PROCESSAMENTO")
    Long somarReservadoEmProcessamento(@Param("produtoId") Long produtoId);
}
