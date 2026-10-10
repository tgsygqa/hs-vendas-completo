package com.hardwarestore.venda.repository;

import com.hardwarestore.venda.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // RF0025 (modulo Cliente) - consulta de todas as transacoes de um
    // cliente. Metodo mantido com o mesmo nome e assinatura de antes, so
    // que agora devolvendo o Pedido completo em vez da versao minima.
    List<Pedido> findByClienteId(Long clienteId);

    // Regra complementar (protege RF0025) - usada pelo ClienteService para
    // bloquear a exclusao definitiva de um cliente que possui pedidos.
    boolean existsByClienteId(Long clienteId);
}
