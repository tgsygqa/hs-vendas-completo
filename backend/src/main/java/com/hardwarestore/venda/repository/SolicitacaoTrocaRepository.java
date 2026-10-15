package com.hardwarestore.venda.repository;

import com.hardwarestore.venda.model.SolicitacaoTroca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitacaoTrocaRepository extends JpaRepository<SolicitacaoTroca, Long> {

    // RF0043 - o administrador visualiza as trocas; mais recente primeiro.
    List<SolicitacaoTroca> findAllByOrderByIdDesc();
}
