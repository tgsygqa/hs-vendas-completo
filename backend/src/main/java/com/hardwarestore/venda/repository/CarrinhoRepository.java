package com.hardwarestore.venda.repository;

import com.hardwarestore.venda.model.Carrinho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CarrinhoRepository extends JpaRepository<Carrinho, Long> {

    // Cada cliente tem no maximo um carrinho aberto (ver Carrinho.cliente,
    // unique = true) - usado pelo CarrinhoService (fase 3) no padrao
    // "busca ou cria" ao adicionar o primeiro item.
    Optional<Carrinho> findByClienteId(Long clienteId);

    // RN0044/RN0045 - ids dos carrinhos que tem ao menos um item cujo prazo
    // de bloqueio ja acabou. Usado pelo job de expiracao.
    @Query("SELECT DISTINCT i.carrinho.id FROM ItemCarrinho i "
            + "WHERE i.bloqueadoAte IS NOT NULL AND i.bloqueadoAte < :agora")
    List<Long> findIdsComItemVencido(@Param("agora") LocalDateTime agora);
}
