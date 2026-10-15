package com.hardwarestore.venda.repository;

import com.hardwarestore.venda.model.ItemCarrinho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ItemCarrinhoRepository extends JpaRepository<ItemCarrinho, Long> {

    // RN0044 - quantidade deste produto que esta bloqueada (reservada) em
    // carrinhos de OUTROS clientes, com prazo ainda valido. E isso que
    // reduz a quantidade que um cliente enxerga como disponivel.
    @Query("SELECT COALESCE(SUM(i.quantidade), 0L) FROM ItemCarrinho i "
            + "WHERE i.produto.id = :produtoId AND i.carrinho.cliente.id <> :clienteId "
            + "AND i.bloqueadoAte > :agora")
    Long somarReservadoPorOutros(@Param("produtoId") Long produtoId,
                                 @Param("clienteId") Long clienteId,
                                 @Param("agora") LocalDateTime agora);
}
