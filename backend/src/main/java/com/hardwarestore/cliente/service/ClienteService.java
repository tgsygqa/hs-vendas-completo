package com.hardwarestore.cliente.service;

import com.hardwarestore.cliente.dto.AlterarSenhaRequestDTO;
import com.hardwarestore.cliente.dto.CartaoRequestDTO;
import com.hardwarestore.cliente.dto.CartaoResponseDTO;
import com.hardwarestore.cliente.dto.ClienteAlteracaoRequestDTO;
import com.hardwarestore.cliente.dto.ClienteCadastroRequestDTO;
import com.hardwarestore.cliente.dto.ClienteResponseDTO;
import com.hardwarestore.cliente.dto.EnderecoRequestDTO;
import com.hardwarestore.cliente.dto.EnderecoResponseDTO;
import com.hardwarestore.cliente.dto.TransacaoResponseDTO;
import com.hardwarestore.cliente.model.Bandeira;
import com.hardwarestore.cliente.model.CartaoCredito;
import com.hardwarestore.cliente.model.Cliente;
import com.hardwarestore.cliente.model.Endereco;
import com.hardwarestore.cliente.model.Telefone;
import com.hardwarestore.cliente.repository.ClienteRepository;
import com.hardwarestore.common.exception.RecursoNaoEncontradoException;
import com.hardwarestore.common.exception.RegraDeNegocioException;
import com.hardwarestore.venda.repository.PedidoRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Concentra todas as regras de negocio do modulo de Gestao de Clientes.
 * Os Controllers nao tomam nenhuma decisao de negocio - apenas delegam
 * para os metodos abaixo e traduzem o resultado em HTTP.
 */
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;
    private final PasswordEncoder passwordEncoder;

    // Injecao de dependencia por construtor
    public ClienteService(ClienteRepository clienteRepository, PedidoRepository pedidoRepository) {
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // ========================================================================
    // RF0021 - Cadastrar cliente
    // ========================================================================
    @Transactional
    public ClienteResponseDTO cadastrar(ClienteCadastroRequestDTO dto) {
        // RN0021/RN0022/RF0021 - CPF e e-mail nao podem existir previamente
        if (clienteRepository.existsByCpf(dto.getCpf())) {
            throw new RegraDeNegocioException("Ja existe um cliente cadastrado com este CPF.");
        }
        if (clienteRepository.existsByEmail(dto.getEmail())) {
            throw new RegraDeNegocioException("Ja existe um cliente cadastrado com este e-mail.");
        }

        // RNF0032 - confirmacao de senha
        if (!dto.getSenha().equals(dto.getConfirmacaoSenha())) {
            throw new RegraDeNegocioException("As senhas nao coincidem.");
        }

        Telefone telefone = new Telefone(dto.getTelefone().getTipo(), dto.getTelefone().getDdd(), dto.getTelefone().getNumero());

        Cliente cliente = Cliente.builder()
                .nome(dto.getNome())
                .cpf(dto.getCpf())
                .genero(dto.getGenero())
                .dataNascimento(dto.getDataNascimento())
                .telefone(telefone)
                .email(dto.getEmail())
                .senha(passwordEncoder.encode(dto.getSenha())) // RNF0033 - senha sempre em hash
                .ativo(true) // RF0021 - cliente sempre nasce ativo
                .ranking(0)  // RN0027 - ranking numerico do cliente
                .build();

        // RN0021/RN0022 - o endereco inicial do cadastro sempre cobre cobranca
        // e entrega ao mesmo tempo, garantindo que as duas regras ja nascem satisfeitas
        Endereco enderecoInicial = Endereco.builder()
                .apelido(dto.getEndereco().getApelido())
                .tipoResidencia(dto.getEndereco().getTipoResidencia())
                .tipoLogradouro(dto.getEndereco().getTipoLogradouro())
                .logradouro(dto.getEndereco().getLogradouro())
                .numero(dto.getEndereco().getNumero())
                .bairro(dto.getEndereco().getBairro())
                .cep(dto.getEndereco().getCep())
                .cidade(dto.getEndereco().getCidade())
                .estado(dto.getEndereco().getEstado())
                .pais(dto.getEndereco().getPais())
                .observacoes(dto.getEndereco().getObservacoes())
                .cobranca(true)
                .entrega(true)
                .cliente(cliente)
                .build();
        cliente.getEnderecos().add(enderecoInicial);

        Cliente salvo = clienteRepository.save(cliente);
        return ClienteResponseDTO.fromEntity(salvo);
    }

    // ========================================================================
    // RF0024 - Consultar clientes com filtros combinaveis/isolados
    // ========================================================================
    public List<ClienteResponseDTO> listar(String nome, String cpf, String email, Boolean ativo) {
        return clienteRepository
                .buscarComFiltros(vazioParaNull(nome), vazioParaNull(cpf), vazioParaNull(email), ativo)
                .stream()
                .map(ClienteResponseDTO::resumo)
                .toList();
    }

    public ClienteResponseDTO buscarPorId(Long id) {
        return ClienteResponseDTO.fromEntity(buscarEntidadePorId(id));
    }

    // ========================================================================
    // RF0022 - Alterar dados cadastrais (senha NAO e alterada aqui - RF0028)
    // ========================================================================
    @Transactional
    public ClienteResponseDTO alterar(Long id, ClienteAlteracaoRequestDTO dto) {
        Cliente cliente = buscarEntidadePorId(id);

        clienteRepository.findByCpf(dto.getCpf())
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new RegraDeNegocioException("Ja existe um cliente cadastrado com este CPF.");
                });
        clienteRepository.findByEmail(dto.getEmail())
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new RegraDeNegocioException("Ja existe um cliente cadastrado com este e-mail.");
                });

        cliente.setNome(dto.getNome());
        cliente.setCpf(dto.getCpf());
        cliente.setGenero(dto.getGenero());
        cliente.setDataNascimento(dto.getDataNascimento());
        cliente.setTelefone(new Telefone(dto.getTelefone().getTipo(), dto.getTelefone().getDdd(), dto.getTelefone().getNumero()));
        cliente.setEmail(dto.getEmail());
        // senha, enderecos e cartoes propositalmente intocados (RNF0034 / RF0028)

        return ClienteResponseDTO.fromEntity(clienteRepository.save(cliente));
    }

    // ========================================================================
    // RF0023 - Inativar / Reativar (soft delete)
    // ========================================================================
    @Transactional
    public ClienteResponseDTO inativar(Long id) {
        Cliente cliente = buscarEntidadePorId(id);
        cliente.setAtivo(false);
        return ClienteResponseDTO.fromEntity(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponseDTO reativar(Long id) {
        Cliente cliente = buscarEntidadePorId(id);
        cliente.setAtivo(true);
        return ClienteResponseDTO.fromEntity(clienteRepository.save(cliente));
    }

    // ========================================================================
    // Exclusao definitiva - regra complementar (nao ha RN explicita no DRS
    // para exclusao fisica de cliente). Existe para nunca invalidar o RF0025
    // real (consulta do historico de transacoes): so e permitida quando o
    // cliente nao possui nenhuma transacao associada; caso contrario, o
    // cliente deve ser inativado (RF0023) em vez de excluido.
    // ========================================================================
    @Transactional
    public void excluir(Long id) {
        Cliente cliente = buscarEntidadePorId(id);

        if (pedidoRepository.existsByClienteId(id)) {
            throw new RegraDeNegocioException(
                    "Este cliente possui transacoes associadas e nao pode ser excluido " +
                    "definitivamente. Utilize a inativacao para preservar o historico (RF0025).");
        }

        clienteRepository.delete(cliente);
    }

    // ========================================================================
    // RF0025 - Consulta de todas as transacoes do cliente, a partir do cadastro
    // ========================================================================
    public List<TransacaoResponseDTO> consultarTransacoes(Long id) {
        buscarEntidadePorId(id); // garante 404 se o cliente nao existir
        return pedidoRepository.findByClienteId(id).stream()
                .map(TransacaoResponseDTO::fromEntity)
                .toList();
    }

    // ========================================================================
    // RF0026 - Enderecos (multiplos, cada um com apelido/frase curta)
    // RN0021/RN0022 - cobranca e entrega | RN0023 - composicao + observacoes opcional
    // RNF0034 - alteracao de enderecos sem precisar editar os demais dados
    // ========================================================================
    @Transactional
    public EnderecoResponseDTO adicionarEndereco(Long clienteId, EnderecoRequestDTO dto) {
        Cliente cliente = buscarEntidadePorId(clienteId);

        // RN0021/RN0022 - o endereco precisa servir para cobranca, entrega, ou ambos
        if (!dto.isCobranca() && !dto.isEntrega()) {
            throw new RegraDeNegocioException("Marque o endereco como cobranca, entrega, ou ambos.");
        }

        Endereco endereco = Endereco.builder()
                .apelido(dto.getApelido())
                .tipoResidencia(dto.getTipoResidencia())
                .tipoLogradouro(dto.getTipoLogradouro())
                .logradouro(dto.getLogradouro())
                .numero(dto.getNumero())
                .bairro(dto.getBairro())
                .cep(dto.getCep())
                .cidade(dto.getCidade())
                .estado(dto.getEstado())
                .pais(dto.getPais())
                .observacoes(dto.getObservacoes())
                .cobranca(dto.isCobranca())
                .entrega(dto.isEntrega())
                .cliente(cliente)
                .build();

        cliente.getEnderecos().add(endereco);
        clienteRepository.saveAndFlush(cliente);
        // save() faz merge: o endereco que o banco numerou (com id) e a copia gerenciada
        // que passa a ocupar o lugar de "endereco" na lista - devolver o objeto original
        // mandaria "id": null para o front, que precisa do id para selecionar o endereco
        // novo na compra (RF0035).
        Endereco persistido = cliente.getEnderecos().get(cliente.getEnderecos().size() - 1);
        return EnderecoResponseDTO.fromEntity(persistido);
    }

    // RN0021/RN0022 - cliente deve manter ao menos um endereco de cobranca e
    // ao menos um de entrega; a remocao e bloqueada se violar qualquer um dos
    // dois (o mesmo endereco pode contar para ambos os papeis).
    @Transactional
    public void removerEndereco(Long clienteId, Long enderecoId) {
        Cliente cliente = buscarEntidadePorId(clienteId);
        Endereco alvo = cliente.getEnderecos().stream()
                .filter(e -> e.getId().equals(enderecoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereco nao encontrado. ID: " + enderecoId));

        boolean restaCobranca = cliente.getEnderecos().stream()
                .anyMatch(e -> !e.getId().equals(enderecoId) && e.isCobranca());
        boolean restaEntrega = cliente.getEnderecos().stream()
                .anyMatch(e -> !e.getId().equals(enderecoId) && e.isEntrega());

        if (!restaCobranca) {
            throw new RegraDeNegocioException("Esta remocao deixaria o cliente sem nenhum endereco de cobranca (RN0021).");
        }
        if (!restaEntrega) {
            throw new RegraDeNegocioException("Esta remocao deixaria o cliente sem nenhum endereco de entrega (RN0022).");
        }

        cliente.getEnderecos().remove(alvo);
        clienteRepository.save(cliente);
    }

    // ========================================================================
    // RF0027 - Cartoes de credito (multiplos, um marcado como preferencial)
    // RN0024 - composicao do registro | RN0025 - bandeira de lista fechada
    // ========================================================================
    @Transactional
    public CartaoResponseDTO adicionarCartao(Long clienteId, CartaoRequestDTO dto) {
        Cliente cliente = buscarEntidadePorId(clienteId);

        // RN0025 - bandeira deve ser de uma lista fechada registrada no sistema
        Bandeira bandeira;
        try {
            bandeira = Bandeira.valueOf(dto.getBandeira().trim().toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException ex) {
            throw new RegraDeNegocioException("Bandeira nao registrada no sistema.");
        }

        boolean ehPrimeiroCartao = cliente.getCartoes().isEmpty();

        // RN0024 - apenas os 4 ultimos digitos do numero sao armazenados;
        // o numero completo e o CVV nunca sao persistidos (so validados no DTO)
        CartaoCredito cartao = CartaoCredito.builder()
                .bandeira(bandeira)
                .ultimosQuatroDigitos(dto.getNumero().substring(dto.getNumero().length() - 4))
                .titular(dto.getTitular())
                .preferencial(ehPrimeiroCartao) // RF0027 - primeiro cartao nasce preferencial
                .cliente(cliente)
                .build();

        cliente.getCartoes().add(cartao);
        clienteRepository.saveAndFlush(cliente);
        // Mesmo motivo de adicionarEndereco: o id esta na copia gerenciada (RF0036).
        CartaoCredito persistido = cliente.getCartoes().get(cliente.getCartoes().size() - 1);
        return CartaoResponseDTO.fromEntity(persistido);
    }

    @Transactional
    public CartaoResponseDTO tornarCartaoPreferencial(Long clienteId, Long cartaoId) {
        Cliente cliente = buscarEntidadePorId(clienteId);
        CartaoCredito alvo = cliente.getCartoes().stream()
                .filter(c -> c.getId().equals(cartaoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cartao nao encontrado. ID: " + cartaoId));

        cliente.getCartoes().forEach(c -> c.setPreferencial(c.getId().equals(cartaoId)));
        clienteRepository.save(cliente);
        return CartaoResponseDTO.fromEntity(alvo);
    }

    @Transactional
    public void removerCartao(Long clienteId, Long cartaoId) {
        Cliente cliente = buscarEntidadePorId(clienteId);
        CartaoCredito alvo = cliente.getCartoes().stream()
                .filter(c -> c.getId().equals(cartaoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cartao nao encontrado. ID: " + cartaoId));

        boolean eraPreferencial = alvo.isPreferencial();
        cliente.getCartoes().remove(alvo);

        // RF0027 - se havia mais de um cartao e o preferencial foi removido, promove o proximo
        if (eraPreferencial && !cliente.getCartoes().isEmpty()) {
            cliente.getCartoes().get(0).setPreferencial(true);
        }
        clienteRepository.save(cliente);
    }

    // ========================================================================
    // RF0028 - Alteracao de senha isolada (sem editar os demais dados cadastrais)
    // ========================================================================
    @Transactional
    public void alterarSenha(Long clienteId, AlterarSenhaRequestDTO dto) {
        Cliente cliente = buscarEntidadePorId(clienteId);

        // RNF0032 - confirmacao de senha
        if (!dto.getNovaSenha().equals(dto.getConfirmacaoSenha())) {
            throw new RegraDeNegocioException("As senhas nao coincidem.");
        }

        // RNF0033 - a nova senha e persistida como hash
        cliente.setSenha(passwordEncoder.encode(dto.getNovaSenha()));
        clienteRepository.save(cliente);
    }

    private Cliente buscarEntidadePorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado. ID: " + id));
    }

    private String vazioParaNull(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor;
    }
}
