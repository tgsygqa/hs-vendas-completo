package com.hardwarestore.venda.repository;

import com.hardwarestore.venda.model.Carrinho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarrinhoRepository extends JpaRepository<Carrinho, Long> {

    // Cada cliente tem no maximo um carrinho aberto (ver Carrinho.cliente,
    // unique = true) - usado pelo CarrinhoService (fase 3) no padrao
    // "busca ou cria" ao adicionar o primeiro item.
    Optional<Carrinho> findByClienteId(Long clienteId);
}
