package com.hardwarestore.cliente.repository;

import com.hardwarestore.cliente.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // RF0021/RF0022 - suporte a regra de unicidade de CPF e e-mail
    Optional<Cliente> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    Optional<Cliente> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * RF0024 - Consulta de clientes com filtros combinaveis e opcionais.
     * Qualquer parametro nulo/ausente e ignorado na clausula WHERE, o que
     * permite usar os filtros de forma isolada ou combinada.
     */
    @Query("""
            SELECT c FROM Cliente c
            WHERE (:nome IS NULL OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
              AND (:cpf IS NULL OR c.cpf LIKE CONCAT('%', :cpf, '%'))
              AND (:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%')))
              AND (:ativo IS NULL OR c.ativo = :ativo)
            """)
    List<Cliente> buscarComFiltros(
            @Param("nome") String nome,
            @Param("cpf") String cpf,
            @Param("email") String email,
            @Param("ativo") Boolean ativo
    );
}
