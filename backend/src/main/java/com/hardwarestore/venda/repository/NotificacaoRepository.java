package com.hardwarestore.venda.repository;

import com.hardwarestore.venda.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByClienteIdOrderByCriadaEmDescIdDesc(Long clienteId);
}
